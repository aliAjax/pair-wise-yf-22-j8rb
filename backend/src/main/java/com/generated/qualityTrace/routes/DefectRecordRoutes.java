package com.generated.qualityTrace.routes;

public final class DefectRecordRoutes {
  public static final String PATH = "/api/defects";
  public static final String DETAIL = PATH + "/{id}";
  public static final String DISPOSITION = DETAIL + "/disposition";
  public static final String CLOSE = DETAIL + "/close";

  private DefectRecordRoutes() {}
}
