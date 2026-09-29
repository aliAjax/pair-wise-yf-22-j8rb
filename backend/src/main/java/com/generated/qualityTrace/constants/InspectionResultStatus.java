package com.generated.qualityTrace.constants;

public enum InspectionResultStatus {
  PASS,
  FAIL,
  CONDITIONAL_PASS,
  RECHECK;

  public int rank() {
    return switch (this) {
      case PASS -> 0;
      case CONDITIONAL_PASS -> 1;
      case RECHECK -> 2;
      case FAIL -> 3;
    };
  }
}
