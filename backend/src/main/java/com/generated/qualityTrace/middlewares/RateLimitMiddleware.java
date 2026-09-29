package com.generated.qualityTrace.middlewares;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** 简单滑动窗口限流：每主体每分钟 N 次。 */
@Component
@Order(5)
public class RateLimitMiddleware extends OncePerRequestFilter {

    private final int limitPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitMiddleware(
            @Value("${app.rate-limit.requests-per-minute}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    private static final class Window {
        long startMillis;
        int count;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);
        long now = System.currentTimeMillis();

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startMillis > 60_000L) {
                Window fresh = new Window();
                fresh.startMillis = now;
                fresh.count = 1;
                return fresh;
            }
            existing.count++;
            return existing;
        });

        if (window.count > limitPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，每分钟限额 "
                            + limitPerMinute + " 次\",\"httpStatus\":429}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "u:" + auth.substring(7).hashCode();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
