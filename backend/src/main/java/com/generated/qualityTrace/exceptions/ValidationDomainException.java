package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

/** 422 入参/业务规则校验失败（区别于 Bean Validation 的 400）。 */
public class ValidationDomainException extends BusinessException {
    public ValidationDomainException(String message) {
        super(ErrorCodes.VALIDATION_FAILED, message, 422);
    }
}
