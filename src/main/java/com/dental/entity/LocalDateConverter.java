package com.dental.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {
  public String convertToDatabaseColumn(LocalDate value) {
    return value == null ? null : value.toString();
  }

  public LocalDate convertToEntityAttribute(String value) {
    return value == null ? null : LocalDate.parse(value);
  }
}
