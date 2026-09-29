package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

public class ValidationException extends ApiException {
  public ValidationException(String message) {
    super(ErrorCodes.VALIDATION_ERROR, message, 400);
  }
}
