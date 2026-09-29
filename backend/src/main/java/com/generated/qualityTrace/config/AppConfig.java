package com.generated.qualityTrace.config;

import com.generated.qualityTrace.middlewares.AuditLogMiddleware;
import com.generated.qualityTrace.middlewares.AuthMiddleware;
import com.generated.qualityTrace.middlewares.RateLimitMiddleware;
import com.generated.qualityTrace.middlewares.RbacMiddleware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {
  private final RateLimitMiddleware rateLimitMiddleware;
  private final AuthMiddleware authMiddleware;
  private final RbacMiddleware rbacMiddleware;
  private final AuditLogMiddleware auditLogMiddleware;

  public AppConfig(
      RateLimitMiddleware rateLimitMiddleware,
      AuthMiddleware authMiddleware,
      RbacMiddleware rbacMiddleware,
      AuditLogMiddleware auditLogMiddleware
  ) {
    this.rateLimitMiddleware = rateLimitMiddleware;
    this.authMiddleware = authMiddleware;
    this.rbacMiddleware = rbacMiddleware;
    this.auditLogMiddleware = auditLogMiddleware;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitMiddleware)
        .addPathPatterns("/api/**");
    registry.addInterceptor(authMiddleware)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/login");
    registry.addInterceptor(rbacMiddleware)
        .addPathPatterns("/api/**");
    registry.addInterceptor(auditLogMiddleware)
        .addPathPatterns("/api/**");
  }
}
