package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 检验项结果路由（只读查询；录入走 /api/inspections 整批事务）。 */
@Component
public class InspectionItemResultRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/inspection-items",
                Set.of(Roles.INSPECTOR, Roles.SUPERVISOR, Roles.MANAGER, Roles.AUDITOR));
    }
}
