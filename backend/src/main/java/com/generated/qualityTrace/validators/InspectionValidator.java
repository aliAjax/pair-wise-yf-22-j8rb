package com.generated.qualityTrace.validators;

import com.generated.qualityTrace.constants.InspectionResultStatus;
import com.generated.qualityTrace.exceptions.ValidationException;
import com.generated.qualityTrace.types.InspectionItemPayload;
import com.generated.qualityTrace.types.QualityInspectionPayload;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class InspectionValidator {
  public InspectionResultStatus validateAndSummarize(QualityInspectionPayload payload) {
    Set<String> itemCodes = new HashSet<>();
    InspectionResultStatus summarized = InspectionResultStatus.PASS;

    for (InspectionItemPayload item : payload.items()) {
      if (!itemCodes.add(item.itemCode())) {
        throw new ValidationException("同一检验内 itemCode 不能重复：" + item.itemCode());
      }
      if (item.limitMin().compareTo(item.limitMax()) > 0) {
        throw new ValidationException("检验项 " + item.itemCode() + " 的下限不能大于上限");
      }

      InspectionResultStatus measuredStatus = item.measuredValue().compareTo(item.limitMin()) < 0
          || item.measuredValue().compareTo(item.limitMax()) > 0
          ? InspectionResultStatus.FAIL
          : InspectionResultStatus.PASS;
      InspectionResultStatus itemStatus = resolveItemStatus(item, measuredStatus);
      if (measuredStatus == InspectionResultStatus.FAIL && itemStatus != InspectionResultStatus.FAIL) {
        throw new ValidationException("检验项 " + item.itemCode() + " 超出规格限，必须判定 FAIL");
      }
      summarized = max(summarized, itemStatus);
    }

    if (payload.resultStatus() != null && !payload.resultStatus().isBlank()) {
      EnumValidator.require(InspectionResultStatus.class, payload.resultStatus(), "resultStatus");
      InspectionResultStatus requested = InspectionResultStatus.valueOf(payload.resultStatus());
      if (requested != summarized) {
        throw new ValidationException(
            "提交结论 " + requested + " 与检验项汇总结果 " + summarized + " 不一致");
      }
    }
    return summarized;
  }

  private InspectionResultStatus resolveItemStatus(
      InspectionItemPayload item,
      InspectionResultStatus measuredStatus
  ) {
    if (item.itemStatus() == null || item.itemStatus().isBlank()) {
      return measuredStatus;
    }
    EnumValidator.require(InspectionResultStatus.class, item.itemStatus(), "itemStatus");
    return InspectionResultStatus.valueOf(item.itemStatus());
  }

  private InspectionResultStatus max(InspectionResultStatus left, InspectionResultStatus right) {
    return left.rank() >= right.rank() ? left : right;
  }
}
