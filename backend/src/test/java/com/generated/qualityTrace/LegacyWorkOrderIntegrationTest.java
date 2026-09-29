package com.generated.qualityTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 无批次号的旧工单仍可查、不强制回填：
 * 种子工单 1001（WO-2025-LEGACY-01）没有任何批次，但列表与详情都能取到。
 */
class LegacyWorkOrderIntegrationTest extends ApiIntegrationTest {

    @BeforeEach
    void setUp() throws Exception {
        resetDb();
    }

    @Test
    void legacyWorkOrderIsListedWithoutBatch() throws Exception {
        String token = asAuditor();

        mockMvc.perform(get("/api/work-orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.orderNo == 'WO-2025-LEGACY-01')]").exists())
                .andExpect(jsonPath("$[?(@.orderNo == 'WO-2025-LEGACY-01')].status")
                        .value(org.hamcrest.Matchers.hasItem("PLANNED")));
    }

    @Test
    void legacyWorkOrderDetailHasNoBatchAndFullEventHistory() throws Exception {
        String token = asAuditor();

        mockMvc.perform(get("/api/work-orders/1001").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("WO-2025-LEGACY-01"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.hasBatch").value(false))
                .andExpect(jsonPath("$.events.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.events[0].toState").value("PLANNED"));
    }
}
