package com.generated.qualityTrace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 测试辅助：@AutoConfigureMockMvc 保证 Auth/Rbac/RateLimit/AuditLog
 * 等 Servlet 过滤器与真实容器一致地挂在 MockMvc 上。
 */
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected ObjectMapper objectMapper;

    protected void resetDb() throws Exception {
        resetDatabase(dataSource);
    }

    protected String login(String username, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    protected String asInspector() throws Exception {
        return login("inspector", "inspector123");
    }

    protected String asSupervisor() throws Exception {
        return login("supervisor", "super123");
    }

    protected String asManager() throws Exception {
        return login("manager", "manager123");
    }

    protected String asAuditor() throws Exception {
        return login("auditor", "auditor123");
    }

    protected String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    protected Map<String, Object> body(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("kv must be pairs");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    protected record RawResult(int status, String body) {}

    protected Map<String, Object> postJson(String path, Object payload, String token) throws Exception {
        return parse(post(path).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(payload)));
    }

    protected Map<String, Object> postJson(String path, Object payload, String token,
                                           String idempotencyKey) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(payload));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return parse(request);
    }

    protected Map<String, Object> getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readMap(result);
    }

    protected RawResult postRaw(String path, Object payload, String token) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        return new RawResult(result.getResponse().getStatus(),
                result.getResponse().getContentAsString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return readMap(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
