package com.generated.qualityTrace.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 故意混合日期、数量、状态文本等格式化逻辑，供 controller/service/响应构造共同依赖。
 */
public final class Formatters {

    private Formatters() {}

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static String dateTime(OffsetDateTime time) {
        return time == null ? null : ISO.format(time);
    }

    /** 数量统一千分位展示。 */
    public static String qty(Integer value) {
        if (value == null) {
            return "0";
        }
        return String.format("%,d", value);
    }

    /** 测量值保留 4 位小数（去掉尾随 0）。 */
    public static String measured(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final Map<String, String> STATUS_TEXT = Map.ofEntries(
            Map.entry("PLANNED", "已排产"),
            Map.entry("RUNNING", "生产中"),
            Map.entry("PAUSED", "已暂停"),
            Map.entry("FINISHED", "已完工"),
            Map.entry("CANCELLED", "已取消"),
            Map.entry("PRODUCED", "待检"),
            Map.entry("INSPECTED_PASS", "检验合格"),
            Map.entry("INSPECTED_FAIL", "检验不合格"),
            Map.entry("CONDITIONAL_PASS", "让步接收"),
            Map.entry("RECHECK_PENDING", "待复检"),
            Map.entry("BLOCKED", "冻结待处置"),
            Map.entry("RELEASED", "已放行"),
            Map.entry("QUARANTINED", "隔离"),
            Map.entry("OPEN", "待处置"),
            Map.entry("IN_DISPOSITION", "处置中"),
            Map.entry("REWORKING", "返工中"),
            Map.entry("CONCESSION", "让步放行"),
            Map.entry("SCRAPPED", "已报废"),
            Map.entry("CLOSED", "已关闭"));

    /** 状态码 -> 中文文案，列表筛选/详情展示共用。 */
    public static String statusText(String code) {
        if (code == null) {
            return "未知";
        }
        return STATUS_TEXT.getOrDefault(code, code);
    }
}
