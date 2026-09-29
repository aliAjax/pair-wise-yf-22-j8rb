package com.generated.qualityTrace.routes;

import com.generated.qualityTrace.constants.Roles;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 不良路由：登记质检员起步；处置/关闭同写矩阵（审计员只读）。 */
@Component
public class DefectRecordRoutes {

    public RouteSpec spec() {
        return new RouteSpec("/api/defects",
                Set.of(Roles.INSPECTOR, Roles.SUPERVISOR, Roles.MANAGER));
    }
}
