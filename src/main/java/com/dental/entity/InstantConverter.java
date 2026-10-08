package com.dental.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;

@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, String> {
  public String convertToDatabaseColumn(Instant value) {
    return value == null ? null : value.toString();
  }

  public Instant convertToEntityAttribute(String value) {
    return value == null ? null : Instant.parse(value);
  }
}
