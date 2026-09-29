package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

public class IllegalBusinessStateException extends ApiException {
  public IllegalBusinessStateException(String message) {
    super(ErrorCodes.ILLEGAL_STATE, message, 422);
  }
}
