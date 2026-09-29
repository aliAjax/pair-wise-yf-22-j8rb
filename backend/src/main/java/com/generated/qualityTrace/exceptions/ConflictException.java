package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

public class ConflictException extends ApiException {
  public ConflictException(String message) {
    super(ErrorCodes.CONFLICT, message, 409);
  }
}
