package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.models.AuditLog;
import com.generated.qualityTrace.repositories.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 审计日志中间件：所有 /api 写操作落 audit_log（失败也记录）。
 * 状态事件本身在 service 内写 state_event，两者互补。
 */
@Component
@Order(20)
public class AuditLogMiddleware extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final AuditLogRepository auditLogRepository;

    public AuditLogMiddleware(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);

        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!path.startsWith("/api") || "GET".equalsIgnoreCase(method)
                || "/api/auth/login".equals(path)) {
            return;
        }

        CurrentUser.Principal principal = CurrentUser.get();
        AuditLog entry = new AuditLog();
        entry.setActor(principal == null ? "anonymous" : principal.username());
        entry.setRole(principal == null ? null : principal.role());
        entry.setAction(method + " " + path);
        entry.setMethod(method);
        entry.setPath(path);
        entry.setResultStatus(response.getStatus());
        try {
            auditLogRepository.insert(entry);
        } catch (Exception e) {
            // 审计落库失败不能掩盖业务结果，仅打日志
            log.warn("audit_log 写入失败 path={} actor={} err={}", path,
                    entry.getActor(), e.getMessage());
        }
    }
}
