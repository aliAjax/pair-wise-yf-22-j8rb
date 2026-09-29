package com.generated.qualityTrace.validators;

import com.generated.qualityTrace.exceptions.ValidationException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProductCodeValidator {
  private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}$");

  public void validate(String productCode) {
    if (!PRODUCT_CODE_PATTERN.matcher(productCode).matches()) {
      throw new ValidationException(
          "产品编码必须为 2-64 位字母、数字、点、下划线或中划线，且以字母或数字开头");
    }
  }
}
