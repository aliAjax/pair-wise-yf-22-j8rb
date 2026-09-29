package com.generated.qualityTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 不良处置：关闭后原判定与变更前后值仍保留；非法流转被拒。
 */
class DefectDispositionIntegrationTest extends ApiIntegrationTest {

    private String supervisorToken;
    private String inspectorToken;
    private String batchNo;

    @BeforeEach
    void setUp() throws Exception {
        resetDb();
        supervisorToken = asSupervisor();
        inspectorToken = asInspector();

        var order = postJson("/api/work-orders",
                body("orderNo", "WO-DEF-1", "productCode", "P-D1", "productName", "缺陷件",
                        "plannedQty", 100, "lineCode", "L-A1"),
                supervisorToken);
        postJson("/api/work-orders/" + order.get("id") + "/start", body(), supervisorToken);
        postJson("/api/batches",
                body("batchNo", "B-DEF-1", "workOrderId", order.get("id"),
                        "quantity", 100, "materialLotNo", "MAT-D"),
                supervisorToken);
        postJson("/api/inspections",
                body("batchNo", "B-DEF-1", "inspectionType", "FIRST", "standardVersion", "STD-D",
                        "qualifiedQty", 90, "defectQty", 0,
                        "items", List.of(body("itemCode", "D1", "itemName", "尺寸",
                                "measuredValue", 5.0, "limitMin", 4.0, "limitMax", 6.0))),
                inspectorToken);

        var defect = postJson("/api/defects",
                body("batchNo", "B-DEF-1", "defectType", "划伤", "defectQty", 5,
                        "severity", "MAJOR", "rootCause", "工装毛刺",
                        "originalResult", "REWORK"),
                inspectorToken);
        batchNo = "B-DEF-1";
        defectId = ((Number) defect.get("id")).longValue();
    }

    private long defectId;

    @Test
    void fullDispositionFlowKeepsOriginalJudgementAfterClose() throws Exception {
        // OPEN -> IN_DISPOSITION -> REWORKING -> CLOSED
        postDisposition("IN_DISPOSITION", null);
        postDisposition("REWORKING", "安排返工");
        var closed = postDisposition("CLOSED", "返工复检合格，关闭");

        assertEquals("CLOSED", closed.get("dispositionStatus"));
        assertEquals(true, closed.get("closed"));
        // 原判定永久保留
        assertEquals("REWORK", closed.get("originalResult"));
        assertEquals("MAJOR", closed.get("originalSeverity"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) closed.get("events");
        assertEquals(4, events.size(), "登记+三次处置，每次都要留记录");
        Map<String, Object> closeEvent = events.get(events.size() - 1);
        assertEquals("OPEN", events.get(0).get("toState"));
        assertEquals("CLOSED", closeEvent.get("toState"));
        @SuppressWarnings("unchecked")
        Map<String, Object> change = (Map<String, Object>) closeEvent.get("change");
        // 关闭事件里同时带原判定与前后值
        assertEquals("REWORKING", change.get("dispositionBefore"));
        assertEquals("CLOSED", change.get("dispositionAfter"));
        assertEquals("REWORK", change.get("originalResult"));
    }

    @Test
    void cannotChangeAfterClosed() throws Exception {
        postDisposition("IN_DISPOSITION", null);
        postDisposition("SCRAPPED", "改判报废");
        postDisposition("CLOSED", "报废结案");

        // 已关闭再推任何流转：409，原判定仍可查
        var rejected = postRaw("/api/defects/" + defectId + "/disposition",
                body("targetStatus", "REWORKING", "dispositionNote", "试图复活"),
                supervisorToken);
        assertEquals(409, rejected.status());

        mockMvcPerformGetDefect().andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.originalResult").value("REWORK"));
    }

    @Test
    void illegalTransitionIsRejected() throws Exception {
        // OPEN 不能直接 CLOSED
        var rejected = postRaw("/api/defects/" + defectId + "/close",
                body("dispositionNote", "直接关闭"), inspectorToken);
        assertEquals(409, rejected.status());
    }

    @Test
    void criticalSeverityCannotTakeConcession() throws Exception {
        var critical = postJson("/api/defects",
                body("batchNo", batchNo, "defectType", "裂纹", "defectQty", 1,
                        "severity", "CRITICAL", "rootCause", "材料夹杂",
                        "originalResult", "SCRAP"),
                inspectorToken);
        long criticalId = ((Number) critical.get("id")).longValue();
        postDispositionFor(criticalId, "IN_DISPOSITION", null, inspectorToken);
        var denied = postRaw("/api/defects/" + criticalId + "/disposition",
                body("targetStatus", "CONCESSION", "dispositionNote", "请求让步"),
                inspectorToken);
        assertEquals(422, denied.status());
    }

    private Map<String, Object> postDisposition(String target, String note) throws Exception {
        return postDispositionFor(defectId, target, note, supervisorToken);
    }

    private Map<String, Object> postDispositionFor(long id, String target, String note,
                                                   String token) throws Exception {
        if ("CLOSED".equals(target)) {
            return postJson("/api/defects/" + id + "/close",
                    body("dispositionNote", note == null ? "关闭" : note), token);
        }
        return postJson("/api/defects/" + id + "/disposition",
                body("targetStatus", target, "dispositionNote", note), token);
    }

    private org.springframework.test.web.servlet.ResultActions mockMvcPerformGetDefect()
            throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/defects/" + defectId)
                .header("Authorization", "Bearer " + supervisorToken));
    }
}
