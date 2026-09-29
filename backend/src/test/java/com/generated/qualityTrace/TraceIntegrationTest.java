package com.generated.qualityTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 全链路追溯：按批次号串起 工单开工 -> 批次投料 -> 检验(含项) -> 不良处置；
 * 当前状态全部由事件流推出；工单状态机也在此覆盖。
 */
class TraceIntegrationTest extends ApiIntegrationTest {

    @BeforeEach
    void setUp() throws Exception {
        resetDb();
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedBatchTracesFullChain() throws Exception {
        String token = asAuditor();
        Map<String, Object> trace = getJson("/api/trace/B2026092801", token);

        assertEquals("B2026092801", trace.get("batchNo"));
        assertEquals("BLOCKED", trace.get("currentStatus"));

        Map<String, Object> wo = (Map<String, Object>) trace.get("workOrder");
        assertNotNull(wo);
        assertEquals("WO-2026-0901-01", wo.get("orderNo"));
        assertEquals("RUNNING", wo.get("currentStatus"));

        List<Map<String, Object>> events = (List<Map<String, Object>>) trace.get("events");
        assertEquals(3, events.size());
        assertEquals("BATCH_CREATED", events.get(0).get("eventType"));
        assertEquals("INSPECTION_PASSED", events.get(1).get("eventType"));
        assertEquals("DEFECT_REGISTERED", events.get(2).get("eventType"));

        List<Map<String, Object>> inspections = (List<Map<String, Object>>) trace.get("inspections");
        assertEquals(1, inspections.size());
        assertEquals("PASS", inspections.get(0).get("resultStatus"));
        assertEquals(3, ((List<?>) inspections.get(0).get("items")).size());

        List<Map<String, Object>> defects = (List<Map<String, Object>>) trace.get("defects");
        assertEquals(1, defects.size());
        assertEquals("SCRAP", defects.get(0).get("originalResult"));
        assertEquals("OPEN", defects.get(0).get("dispositionStatus"));
    }

    @Test
    void batchWithoutWorkOrderStillTraceableWithNullWorkOrder() throws Exception {
        String supervisor = asSupervisor();
        postJson("/api/batches",
                body("batchNo", "B-STANDALONE", "quantity", 10, "materialLotNo", "MAT-S"),
                supervisor);

        Map<String, Object> trace = getJson("/api/trace/B-STANDALONE", supervisor);
        assertNull(trace.get("workOrder"), "无工单批次不强制回填工单");
        assertEquals("PRODUCED", trace.get("currentStatus"));
    }

    @Test
    void workOrderLifecycleIsDerivedFromEvents() throws Exception {
        String supervisor = asSupervisor();
        Map<String, Object> order = postJson("/api/work-orders",
                body("orderNo", "WO-LIFE-1", "productCode", "P-L1", "productName", "生命周期件",
                        "plannedQty", 50, "lineCode", "L-B1"),
                supervisor);
        long id = ((Number) order.get("id")).longValue();

        assertEquals("PLANNED", getJson("/api/work-orders/" + id, supervisor).get("status"));
        assertEquals("RUNNING", postJson("/api/work-orders/" + id + "/start", body(), supervisor).get("status"));
        assertEquals("PAUSED", postJson("/api/work-orders/" + id + "/pause", body(), supervisor).get("status"));
        assertEquals("RUNNING", postJson("/api/work-orders/" + id + "/start", body(), supervisor).get("status"));
        Map<String, Object> finished = postJson("/api/work-orders/" + id + "/finish", body(), supervisor);
        assertEquals("FINISHED", finished.get("status"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) finished.get("events");
        assertEquals(5, events.size(), "创建/开工/暂停/复工/完工 每次都留痕");
    }

    @Test
    void illegalWorkOrderTransitionConflicts() throws Exception {
        String supervisor = asSupervisor();
        Map<String, Object> order = postJson("/api/work-orders",
                body("orderNo", "WO-LIFE-2", "productCode", "P-L2", "productName", "冲突件",
                        "plannedQty", 10, "lineCode", "L-B2"),
                supervisor);
        long id = ((Number) order.get("id")).longValue();

        // PLANNED 不能直接完工
        var result = postRaw("/api/work-orders/" + id + "/finish", body(), supervisor);
        assertEquals(409, result.status());
        assertEquals("PLANNED", getJson("/api/work-orders/" + id, supervisor).get("status"));
    }
}
