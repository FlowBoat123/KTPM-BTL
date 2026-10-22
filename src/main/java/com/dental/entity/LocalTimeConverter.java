package com.dental.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalTime;

@Converter(autoApply = true)
public class LocalTimeConverter implements AttributeConverter<LocalTime, String> {
  public String convertToDatabaseColumn(LocalTime value) {
    return value == null
        ? null
        : value.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
  }

  public LocalTime convertToEntityAttribute(String value) {
    return value == null ? null : LocalTime.parse(value);
  }
}
