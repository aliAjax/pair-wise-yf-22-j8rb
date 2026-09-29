package com.generated.qualityTrace.utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatters {
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

  private Formatters() {}

  public static String instant(Instant value) {
    return value == null ? null : TIME_FORMATTER.format(value);
  }

  public static String quantity(BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  public static String status(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public static String auditTarget(String type, Object id) {
    return type + "#" + id;
  }
}
