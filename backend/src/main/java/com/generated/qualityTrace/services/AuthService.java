package com.generated.qualityTrace.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.exceptions.ApiException;
import com.generated.qualityTrace.exceptions.ServiceLayerException;
import com.generated.qualityTrace.models.User;
import com.generated.qualityTrace.repositories.AuditLogRepository;
import com.generated.qualityTrace.repositories.UserRepository;
import com.generated.qualityTrace.types.ActorContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final UserRepository userRepository;
  private final AuditService auditService;
  private final String jwtSecret;
  private final long ttlSeconds;

  public AuthService(
      UserRepository userRepository,
      AuditService auditService,
      @Value("${app.jwt.secret}") String jwtSecret,
      @Value("${app.jwt.ttl-seconds}") long ttlSeconds
  ) {
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.jwtSecret = jwtSecret;
    this.ttlSeconds = ttlSeconds;
  }

  public Map<String, Object> login(String username, String password, String path) {
    User user = userRepository.findByUsername(username)
        .filter(User::enabled)
        .filter(candidate -> matchesPassword(password, candidate.passwordSalt(), candidate.passwordHash()))
        .orElseThrow(() -> new ApiException(ErrorCodes.BAD_CREDENTIALS, ErrorMessages.BAD_CREDENTIALS, 401));

    Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
    String token = issueToken(user, expiresAt);
    auditService.record(user.username(), "AUTH_LOGIN", "USER", user.id().toString(),
        LogTemplates.USER_LOGIN.formatted(user.username()), path, "POST");

    return Map.of(
        "token", token,
        "tokenType", "Bearer",
        "expiresAt", expiresAt,
        "actor", Map.of(
            "userId", user.id(),
            "username", user.username(),
            "displayName", user.displayName(),
            "role", user.role()
        )
    );
  }

  public ActorContext verify(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3 || !constantTimeEquals(parts[2], sign(parts[0] + "." + parts[1]))) {
        throw invalidToken();
      }
      Map<String, Object> claims = OBJECT_MAPPER.readValue(
          URL_DECODER.decode(parts[1]), new TypeReference<Map<String, Object>>() {});
      long expiresAt = ((Number) claims.get("exp")).longValue();
      if (Instant.now().getEpochSecond() >= expiresAt) {
        throw invalidToken();
      }
      return new ActorContext(
          ((Number) claims.get("uid")).longValue(),
          (String) claims.get("sub"),
          (String) claims.get("role")
      );
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw invalidToken();
    }
  }

  private String issueToken(User user, Instant expiresAt) {
    try {
      Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
      Map<String, Object> claims = new HashMap<>();
      claims.put("uid", user.id());
      claims.put("sub", user.username());
      claims.put("role", user.role());
      claims.put("iat", Instant.now().getEpochSecond());
      claims.put("exp", expiresAt.getEpochSecond());
      String headerPart = URL_ENCODER.encodeToString(
          OBJECT_MAPPER.writeValueAsBytes(header));
      String payloadPart = URL_ENCODER.encodeToString(
          OBJECT_MAPPER.writeValueAsBytes(claims));
      String content = headerPart + "." + payloadPart;
      return content + "." + sign(content);
    } catch (Exception e) {
      throw new ServiceLayerException("JWT 生成失败", e);
    }
  }

  private String sign(String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new ServiceLayerException("JWT 签名失败", e);
    }
  }

  private boolean matchesPassword(String rawPassword, String salt, String expectedHash) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] hash = factory.generateSecret(new PBEKeySpec(
          rawPassword.toCharArray(),
          Base64.getDecoder().decode(salt),
          12000,
          256
      )).getEncoded();
      return MessageDigest.isEqual(
          Base64.getDecoder().decode(expectedHash),
          hash
      );
    } catch (Exception e) {
      throw new ServiceLayerException("密码校验失败", e);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8)
    );
  }

  private ApiException invalidToken() {
    return new ApiException(ErrorCodes.INVALID_TOKEN, ErrorMessages.INVALID_TOKEN, 401);
  }
}
