package com.generated.qualityTrace.middlewares;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.exceptions.BusinessException;
import com.generated.qualityTrace.routes.RouteRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * RBAC 中间件：按 routes 包登记的路由矩阵做角色校验。
 * 必须排在 AuthMiddleware 之后。过滤器中的异常到不了 @RestControllerAdvice，
 * 因此这里直接渲染业务错误出参（不在全局异常处吞掉错误码）。
 */
@Component
@Order(11)
public class RbacMiddleware extends OncePerRequestFilter {

    private final RouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;

    public RbacMiddleware(RouteRegistry routeRegistry, ObjectMapper objectMapper) {
        this.routeRegistry = routeRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        try {
            if (!"GET".equalsIgnoreCase(method)) {
                CurrentUser.Principal principal = CurrentUser.get();
                // 公共路径在 AuthMiddleware 已直接放行；走到这里无主体说明配置遗漏
                if (principal != null) {
                    Set<String> required = routeRegistry.requiredWriteRoles(path);
                    if (required != null && !required.contains(principal.role())) {
                        writeError(response, 403, "RBAC_DENIED", String.format(
                                ErrorMessages.RBAC_DENIED, principal.role(), method + " " + path));
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            writeError(response, e.getHttpStatus(), e.getCode(), e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("httpStatus", status);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
