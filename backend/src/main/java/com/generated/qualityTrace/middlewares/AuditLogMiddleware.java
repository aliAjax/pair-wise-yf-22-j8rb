package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.services.AuditService;
import com.generated.qualityTrace.types.ActorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditLogMiddleware implements HandlerInterceptor {
  private final AuditService auditService;

  public AuditLogMiddleware(AuditService auditService) {
    this.auditService = auditService;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    if (!isWriteMethod(request.getMethod()) || request.getRequestURI().startsWith("/actuator")) {
      return;
    }
    ActorContext actor = (ActorContext) request.getAttribute(AuthMiddleware.ACTOR_ATTRIBUTE);
    String actorName = actor == null ? "anonymous" : actor.username();
    String action = "HTTP_" + request.getMethod() + "_" + response.getStatus();
    String message = "%s %s -> %d".formatted(
        request.getMethod(), request.getRequestURI(), response.getStatus());
    auditService.record(
        actorName,
        action,
        "HTTP_REQUEST",
        request.getRequestURI(),
        message,
        request.getRequestURI(),
        request.getMethod()
    );
  }

  private boolean isWriteMethod(String method) {
    return "POST".equalsIgnoreCase(method)
        || "PUT".equalsIgnoreCase(method)
        || "PATCH".equalsIgnoreCase(method)
        || "DELETE".equalsIgnoreCase(method);
  }
}
