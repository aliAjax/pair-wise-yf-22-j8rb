package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constructors.InspectionItemResultDtoFactory;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.exceptions.NotFoundException;
import com.generated.qualityTrace.repositories.InspectionItemResultRepository;
import com.generated.qualityTrace.repositories.QualityInspectionRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InspectionItemResultService {
  private final InspectionItemResultRepository inspectionItemResultRepository;
  private final QualityInspectionRepository qualityInspectionRepository;

  public InspectionItemResultService(
      InspectionItemResultRepository inspectionItemResultRepository,
      QualityInspectionRepository qualityInspectionRepository
  ) {
    this.inspectionItemResultRepository = inspectionItemResultRepository;
    this.qualityInspectionRepository = qualityInspectionRepository;
  }

  public List<Map<String, Object>> listByInspection(Long inspectionId) {
    if (qualityInspectionRepository.findById(inspectionId).isEmpty()) {
      throw new NotFoundException(ErrorMessages.INSPECTION_NOT_FOUND.formatted(inspectionId));
    }
    return inspectionItemResultRepository.findByInspectionId(inspectionId).stream()
        .map(InspectionItemResultDtoFactory::response)
        .toList();
  }
}
