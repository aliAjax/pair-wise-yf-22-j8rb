package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 批次路由：投料仅产线主管与质量经理。 */
@Component
public class ProductBatchRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/batches",
                Set.of(Roles.SUPERVISOR, Roles.MANAGER));
    }
}
