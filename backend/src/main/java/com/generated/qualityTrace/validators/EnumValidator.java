package com.generated.qualityTrace.validators;

import java.util.Arrays;

public final class EnumValidator {
  private EnumValidator() {}

  public static <E extends Enum<E>> boolean contains(Class<E> enumType, String value) {
    if (value == null) {
      return false;
    }
    return Arrays.stream(enumType.getEnumConstants())
        .map(Enum::name)
        .anyMatch(name -> name.equals(value));
  }

  public static <E extends Enum<E>> void require(Class<E> enumType, String value, String field) {
    if (!contains(enumType, value)) {
      throw new com.generated.qualityTrace.exceptions.ValidationException(
          field + " 必须是 " + Arrays.toString(enumType.getEnumConstants()) + " 之一");
    }
  }
}
