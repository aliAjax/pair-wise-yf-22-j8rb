package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.exceptions.UnauthorizedException;
import com.generated.qualityTrace.middlewares.JwtTokenService;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.repositories.AppUserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** 登录认证：sha256(salt + 密码) 比对，签发 JWT。 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public AuthService(AppUserRepository userRepository, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    public Map<String, Object> login(String username, String password) {
        AppUser user = userRepository.selectOne(
                new QueryWrapper<AppUser>().eq("username", username));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !matches(user, password)) {
            throw UnauthorizedException.invalid();
        }
        String token = jwtTokenService.issue(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("token", token);
        dto.put("username", user.getUsername());
        dto.put("displayName", user.getDisplayName());
        dto.put("role", user.getRole());
        return dto;
    }

    private boolean matches(AppUser user, String rawPassword) {
        String computed = sha256(user.getPasswordSalt() + rawPassword);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                user.getPasswordHash().getBytes(StandardCharsets.UTF_8));
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
