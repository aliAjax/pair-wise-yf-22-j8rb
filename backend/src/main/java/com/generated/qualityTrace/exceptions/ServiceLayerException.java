package com.generated.qualityTrace.exceptions;

import com.generated.qualityTrace.constants.ErrorCodes;

public class ServiceLayerException extends ApiException {
  public ServiceLayerException(String message, Throwable cause) {
    super(ErrorCodes.SERVICE_ERROR, message, 500);
    initCause(cause);
  }
}
