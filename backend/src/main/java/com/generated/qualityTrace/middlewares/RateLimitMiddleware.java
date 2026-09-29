package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitMiddleware implements HandlerInterceptor {
  private final int limitPerMinute;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitMiddleware(@Value("${app.rate-limit.requests-per-minute}") int limitPerMinute) {
    this.limitPerMinute = limitPerMinute;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    long minute = Instant.now().getEpochSecond() / 60;
    String key = clientIp(request) + "|" + request.getMethod() + "|" + request.getRequestURI();
    Window window = windows.compute(key, (ignored, existing) -> {
      if (existing == null || existing.minute != minute) {
        return new Window(minute, 1);
      }
      existing.count++;
      return existing;
    });
    if (window.count > limitPerMinute) {
      response.setHeader("Retry-After", "60");
      throw new ApiException(ErrorCodes.RATE_LIMITED, ErrorMessages.RATE_LIMITED, 429);
    }
    response.setHeader("X-RateLimit-Limit", String.valueOf(limitPerMinute));
    response.setHeader("X-RateLimit-Remaining",
        String.valueOf(Math.max(0, limitPerMinute - window.count)));
    return true;
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static final class Window {
    private final long minute;
    private int count;

    private Window(long minute, int count) {
      this.minute = minute;
      this.count = count;
    }
  }
}
