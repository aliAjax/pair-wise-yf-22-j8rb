package com.generated.qualityTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 同一批次多条检验并发回传：
 * 后到的必须接住前一批的状态与数量（累加接续），不能覆盖。
 */
class ConcurrentInspectionIntegrationTest extends ApiIntegrationTest {

    private String supervisorToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() throws Exception {
        resetDb();
        supervisorToken = asSupervisor();
        inspectorToken = asInspector();
    }

    private Map<String, Object> createOrderAndBatch(String batchNo, int qty) throws Exception {
        var orderResp = postJson("/api/work-orders",
                body("orderNo", "WO-CONC-" + batchNo,
                        "productCode", "P-X1", "productName", "并发件",
                        "plannedQty", qty, "lineCode", "L-A1"),
                supervisorToken);
        Long orderId = ((Number) orderResp.get("id")).longValue();
        postJson("/api/work-orders/" + orderId + "/start", body(), supervisorToken);
        var batchResp = postJson("/api/batches",
                body("batchNo", batchNo, "workOrderId", orderId,
                        "quantity", qty, "materialLotNo", "MAT-C"),
                supervisorToken);
        return batchResp;
    }

    private List<Map<String, Object>> items(double... measured) {
        return java.util.Arrays.stream(measured)
                .mapToObj(v -> body("itemCode", "D" + v, "itemName", "尺寸",
                        "measuredValue", v, "limitMin", 10.0, "limitMax", 20.0))
                .toList();
    }

    @Test
    @SuppressWarnings("unchecked")
    void concurrentInspectionsAccumulateInsteadOfOverwriting() throws Exception {
        int qty = 1000;
        String batchNo = "B-CONC-1";
        createOrderAndBatch(batchNo, qty);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    try {
                        postJson("/api/inspections",
                                body("batchNo", batchNo,
                                        "inspectionType", "PATROL",
                                        "standardVersion", "STD-1",
                                        "qualifiedQty", 50,
                                        "defectQty", 1,
                                        "items", items(15.0 + idx * 0.01)),
                                inspectorToken, "idem-" + idx);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
        }

        // 数量必须是 8 批的累加，而不是最后一条覆盖成 50/1
        Map<String, Object> batch = getJson("/api/batches/" + batchNo, inspectorToken);
        assertEquals(400, ((Number) batch.get("qualifiedQty")).intValue());
        assertEquals(8, ((Number) batch.get("defectQty")).intValue());

        // 事件流保留每一次状态变化
        Map<String, Object> trace = getJson("/api/trace/" + batchNo, inspectorToken);
        List<Map<String, Object>> events = (List<Map<String, Object>>) trace.get("events");
        assertEquals(threads, events.stream()
                .filter(e -> "INSPECTION_PASSED".equals(e.get("eventType")))
                .count());

        // 每条事件都要看到“前值 -> 后值”的接续
        Map<String, Object> lastPass = events.stream()
                .filter(e -> "INSPECTION_PASSED".equals(e.get("eventType")))
                .reduce((a, b) -> b).orElseThrow();
        Map<String, Object> change = (Map<String, Object>) lastPass.get("change");
        assertEquals(350, ((Number) change.get("qualifiedQtyBefore")).intValue());
        assertEquals(400, ((Number) change.get("qualifiedQtyAfter")).intValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sameIdempotencyKeyReplaysWithoutDoubleCounting() throws Exception {
        String batchNo = "B-CONC-2";
        createOrderAndBatch(batchNo, 100);

        var payload = body("batchNo", batchNo, "inspectionType", "FIRST",
                "standardVersion", "STD-1", "qualifiedQty", 10, "defectQty", 0,
                "items", items(15.0));

        Map<String, Object> first = postJson("/api/inspections", payload,
                inspectorToken, "SAME-KEY");
        Map<String, Object> second = postJson("/api/inspections", payload,
                inspectorToken, "SAME-KEY");

        assertEquals(first.get("id"), second.get("id"));
        Map<String, Object> batch = getJson("/api/batches/" + batchNo, inspectorToken);
        assertEquals(10, ((Number) batch.get("qualifiedQty")).intValue());
    }

    @Test
    void failedInspectionLeavesNoPartialRecords() throws Exception {
        String batchNo = "B-CONC-3";
        createOrderAndBatch(batchNo, 100);

        // 合格 90 + 不良 20 = 110 > 批次 100：数量校验失败
        var badPayload = body("batchNo", batchNo, "inspectionType", "FINAL",
                "standardVersion", "STD-1", "qualifiedQty", 90, "defectQty", 20,
                "items", items(15.0));
        var result = postRaw("/api/inspections", badPayload, inspectorToken);
        assertEquals(422, result.status());
        assertTrue(result.body().contains("VALIDATION_FAILED"));

        Integer inspectionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM quality_inspection qi "
                        + "JOIN product_batch pb ON pb.id = qi.batch_id "
                        + "WHERE pb.batch_no = ?", Integer.class, batchNo);
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inspection_item_result iir "
                        + "JOIN quality_inspection qi ON qi.id = iir.inspection_id "
                        + "JOIN product_batch pb ON pb.id = qi.batch_id "
                        + "WHERE pb.batch_no = ?", Integer.class, batchNo);
        Integer batchEvents = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM state_event se "
                        + "JOIN product_batch pb ON pb.id = se.aggregate_id "
                        + "WHERE se.aggregate_type = 'BATCH' AND pb.batch_no = ?",
                Integer.class, batchNo);
        assertEquals(0, inspectionCount, "失败检验不留主表记录");
        assertEquals(0, itemCount, "失败检验不留检验项记录");
        assertEquals(1, batchEvents, "只保留 BATCH_CREATED，一条检验事件都不能多");
    }
}
