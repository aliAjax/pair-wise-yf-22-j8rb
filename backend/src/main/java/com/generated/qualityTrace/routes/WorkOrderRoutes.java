package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 工单路由：开工/暂停/完工仅产线主管与质量经理。 */
@Component
public class WorkOrderRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/work-orders",
                Set.of(Roles.SUPERVISOR, Roles.MANAGER));
    }
}
