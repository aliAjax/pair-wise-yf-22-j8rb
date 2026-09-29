package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

/** 404 资源不存在。 */
public class NotFoundException extends BusinessException {
    public NotFoundException(String code, String message) {
        super(code, message, 404);
    }

    public static NotFoundException workOrder(String orderNo) {
        return new NotFoundException(ErrorCodes.WORK_ORDER_NOT_FOUND,
                String.format("工单不存在：%s", orderNo));
    }

    public static NotFoundException batch(String batchNo) {
        return new NotFoundException(ErrorCodes.BATCH_NOT_FOUND,
                String.format("批次不存在：%s", batchNo));
    }

    public static NotFoundException defect(Long id) {
        return new NotFoundException(ErrorCodes.DEFECT_NOT_FOUND,
                String.format("不良记录不存在：%s", id));
    }
}
