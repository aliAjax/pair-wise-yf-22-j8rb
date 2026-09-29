package com.generated.qualityTrace.validators;

import com.generated.qualityTrace.constants.DefectSeverity;
import com.generated.qualityTrace.constants.InspectionResultStatus;
import com.generated.qualityTrace.exceptions.ValidationDomainException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 入参之外的领域规则校验器（枚举合法性、判定与检验项一致性、限值顺序等）。
 */
public final class DomainValidator {

    private DomainValidator() {}

    private static final Set<String> INSPECTION_TYPES = Set.of("FIRST", "PATROL", "FINAL");
    private static final Set<String> DISPOSITION_RESULTS = Set.of("REWORK", "CONCESSION", "SCRAP");

    public static void requireInspectionType(String type) {
        if (!INSPECTION_TYPES.contains(type)) {
            throw new ValidationDomainException("检验类型必须是 FIRST/PATROL/FINAL，实际：" + type);
        }
    }

    public static void requireSeverity(String severity) {
        try {
            DefectSeverity.valueOf(severity);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("严重度必须是 MINOR/MAJOR/CRITICAL，实际：" + severity);
        }
    }

    public static void requireOriginalResult(String result) {
        if (!DISPOSITION_RESULTS.contains(result)) {
            throw new ValidationDomainException("原判定必须是 REWORK/CONCESSION/SCRAP，实际：" + result);
        }
    }

    public static void requireLimitOrder(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new ValidationDomainException(
                    String.format("检验限值非法：下限 %s 大于上限 %s", min, max));
        }
    }

    /** 根据实测值与限值判定检验项状态。 */
    public static String judgeItem(BigDecimal measured, BigDecimal min, BigDecimal max) {
        requireLimitOrder(min, max);
        return measured.compareTo(min) >= 0 && measured.compareTo(max) <= 0 ? "PASS" : "FAIL";
    }

    /**
     * 根据所有检验项状态与数量汇总本次检验结论：
     * 任一 FAIL -> FAIL；全部 PASS -> PASS。
     */
    public static InspectionResultStatus judgeInspection(Set<String> itemStatuses,
                                                         int recheckQty) {
        if (itemStatuses.contains("FAIL")) {
            return InspectionResultStatus.FAIL;
        }
        if (recheckQty > 0) {
            return InspectionResultStatus.RECHECK;
        }
        return InspectionResultStatus.PASS;
    }

    public static String enumCsv(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining("/"));
    }
}
