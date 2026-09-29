package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.exceptions.ApiException;
import com.generated.qualityTrace.services.AuthService;
import com.generated.qualityTrace.types.ActorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthMiddleware implements HandlerInterceptor {
  public static final String ACTOR_ATTRIBUTE = "qualityTrace.actor";

  private final AuthService authService;

  public AuthMiddleware(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new ApiException(ErrorCodes.AUTH_REQUIRED, ErrorMessages.AUTH_REQUIRED, 401);
    }
    String token = authorization.substring("Bearer ".length()).trim();
    ActorContext actor = authService.verify(token);
    request.setAttribute(ACTOR_ATTRIBUTE, actor);
    return true;
  }
}
