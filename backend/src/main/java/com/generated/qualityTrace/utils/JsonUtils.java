package com.generated.qualityTrace.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generated.qualityTrace.exceptions.ServiceLayerException;

public final class JsonUtils {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtils() {}

  public static String write(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new ServiceLayerException("状态事件 JSON 序列化失败", e);
    }
  }

  public static Object readTree(String json) {
    try {
      return OBJECT_MAPPER.readTree(json);
    } catch (JsonProcessingException e) {
      throw new ServiceLayerException("状态事件 JSON 解析失败", e);
    }
  }
}
