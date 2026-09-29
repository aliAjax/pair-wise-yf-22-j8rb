package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.UserRole;
import com.generated.qualityTrace.exceptions.ApiException;
import com.generated.qualityTrace.types.ActorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RbacMiddleware implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    RequireRoles methodRoles = handlerMethod.getMethodAnnotation(RequireRoles.class);
    RequireRoles classRoles = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
    RequireRoles roles = methodRoles != null ? methodRoles : classRoles;
    if (roles == null) {
      return true;
    }

    ActorContext actor = (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
    UserRole currentRole = UserRole.valueOf(actor.role());
    boolean allowed = Arrays.asList(roles.value()).contains(currentRole);
    if (!allowed) {
      throw new ApiException(ErrorCodes.RBAC_DENIED, ErrorMessages.RBAC_DENIED, 403);
    }
    return true;
  }
}
