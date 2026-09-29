package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

/** 401 未认证。 */
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String code, String message) {
        super(code, message, 401);
    }

    public static UnauthorizedException missing() {
        return new UnauthorizedException(ErrorCodes.AUTH_REQUIRED, "缺少认证令牌，请先登录");
    }

    public static UnauthorizedException invalid() {
        return new UnauthorizedException(ErrorCodes.AUTH_INVALID_TOKEN, "认证令牌无效或已过期");
    }
}
