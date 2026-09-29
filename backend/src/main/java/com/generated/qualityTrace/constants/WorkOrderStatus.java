package com.generated.qualityTrace.constants;

public enum WorkOrderStatus {
  PLANNED,
  RUNNING,
  PAUSED,
  FINISHED,
  CANCELLED;

  public boolean canTransitionTo(WorkOrderStatus target) {
    return switch (this) {
      case PLANNED -> target == RUNNING || target == CANCELLED;
      case RUNNING -> target == PAUSED || target == FINISHED || target == CANCELLED;
      case PAUSED -> target == RUNNING || target == CANCELLED;
      case FINISHED, CANCELLED -> false;
    };
  }
}
