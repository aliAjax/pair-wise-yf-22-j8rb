package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 检验路由：检验录入为质检员与质量经理。 */
@Component
public class QualityInspectionRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/inspections",
                Set.of(Roles.INSPECTOR, Roles.MANAGER));
    }
}
