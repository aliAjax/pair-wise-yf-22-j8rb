package com.generated.qualityTrace.middlewares;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generated.qualityTrace.constants.ErrorMessages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 认证中间件：校验 JWT 并把主体放入线程上下文。
 * 角色判定交给 RbacMiddleware（职责分开，不在这里吞掉 403）。
 */
@Component
@Order(10)
public class AuthMiddleware extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public AuthMiddleware(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublic(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "AUTH_REQUIRED", ErrorMessages.AUTH_REQUIRED);
            return;
        }
        try {
            CurrentUser.set(jwtTokenService.parse(header.substring(7)));
            chain.doFilter(request, response);
        } catch (Exception e) {
            if (e.getClass().getSimpleName().contains("Jwt") || e instanceof IllegalArgumentException) {
                writeError(response, 401, "AUTH_INVALID_TOKEN", ErrorMessages.AUTH_INVALID_TOKEN);
                return;
            }
            throw e;
        } finally {
            CurrentUser.clear();
        }
    }

    private boolean isPublic(String path) {
        return "/health".equals(path)
                || path.startsWith("/actuator")
                || "/api/auth/login".equals(path);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "code", code,
                "message", message,
                "httpStatus", status)));
    }
}
