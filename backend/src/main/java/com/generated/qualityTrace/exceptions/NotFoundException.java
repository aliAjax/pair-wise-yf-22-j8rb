package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

public class NotFoundException extends ApiException {
  public NotFoundException(String message) {
    super(ErrorCodes.NOT_FOUND, message, 404);
  }
}
