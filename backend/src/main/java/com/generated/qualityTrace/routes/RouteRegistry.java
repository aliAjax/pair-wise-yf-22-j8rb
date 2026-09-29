package com.generated.qualityTrace.routes;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 路由表：按实体分文件登记，RbacMiddleware 统一读取。
 * GET 对任意已认证角色开放（AUDITOR 只读）；写操作按下列矩阵限制。
 */
@Component
public class RouteRegistry {

    private final List<RouteSpec> specs;

    public RouteRegistry(WorkOrderRoutes workOrderRoutes,
                         ProductBatchRoutes productBatchRoutes,
                         QualityInspectionRoutes qualityInspectionRoutes,
                         InspectionItemResultRoutes inspectionItemResultRoutes,
                         DefectRecordRoutes defectRecordRoutes,
                         TraceRoutes traceRoutes) {
        this.specs = List.of(
                workOrderRoutes.spec(),
                productBatchRoutes.spec(),
                qualityInspectionRoutes.spec(),
                inspectionItemResultRoutes.spec(),
                defectRecordRoutes.spec(),
                traceRoutes.spec());
    }

    /** 返回写该路径所需角色；无匹配返回 null（放行到后续规则）。 */
    public Set<String> requiredWriteRoles(String path) {
        for (RouteSpec spec : specs) {
            if (spec.covers(path)) {
                return spec.writeRoles();
            }
        }
        return null;
    }
}
