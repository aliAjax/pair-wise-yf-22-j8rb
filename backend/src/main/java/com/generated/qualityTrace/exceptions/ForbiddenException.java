package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

/** 403 角色不足。 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(ErrorCodes.RBAC_DENIED, message, 403);
    }
}
