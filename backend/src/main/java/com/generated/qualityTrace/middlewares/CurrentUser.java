package com.generated.qualityTrace.middlewares;

/** 当前请求认证主体，线程内传递。 */
public class CurrentUser {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    public record Principal(Long userId, String username, String role) {}

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal get() {
        return HOLDER.get();
    }

    public static String username() {
        Principal p = HOLDER.get();
        return p == null ? "anonymous" : p.username();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private CurrentUser() {}
}
