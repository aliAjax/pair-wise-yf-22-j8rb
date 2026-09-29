package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

/** 409 业务冲突：状态不允许流转、数量超限、重复提交等。 */
public class ConflictException extends BusinessException {
    public ConflictException(String code, String message) {
        super(code, message, 409);
    }

    public static ConflictException idempotencyMismatch(String key) {
        return new ConflictException(ErrorCodes.IDEMPOTENCY_MISMATCH,
                String.format("幂等键 %s 对应的请求内容不一致", key));
    }
}
