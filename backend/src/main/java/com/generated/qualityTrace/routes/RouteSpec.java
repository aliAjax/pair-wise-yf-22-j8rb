package com.generated.qualityTrace.routes;

import java.util.Set;

/** 一条路由规格：路径前缀 + 写操作允许的角色集合。 */
public record RouteSpec(String pathPrefix, Set<String> writeRoles) {

    public boolean covers(String path) {
        return path.startsWith(pathPrefix);
    }
}
