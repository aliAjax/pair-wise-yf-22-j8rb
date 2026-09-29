package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 追溯路由：只读，所有角色可查。 */
@Component
public class TraceRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/trace",
                Set.of(Roles.INSPECTOR, Roles.SUPERVISOR, Roles.MANAGER, Roles.AUDITOR));
    }
}
