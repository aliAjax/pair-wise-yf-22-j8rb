package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constructors.QualityInspectionDtoFactory;
import com.generated.qualityTrace.models.InspectionItemResult;
import com.generated.qualityTrace.repositories.InspectionItemResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 检验项结果只读查询（写入随检验整批事务完成）。 */
@Service
public class InspectionItemResultService {

    private final InspectionItemResultRepository itemRepository;

    public InspectionItemResultService(InspectionItemResultRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Map<String, Object>> listByInspection(Long inspectionId) {
        return itemRepository.selectList(
                        new QueryWrapper<InspectionItemResult>()
                                .eq("inspection_id", inspectionId)
                                .orderByAsc("id")).stream()
                .map(QualityInspectionDtoFactory::item)
                .toList();
    }
}
