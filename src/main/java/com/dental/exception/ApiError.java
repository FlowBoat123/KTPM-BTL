package com.dental.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String message, Map<String, String> errors) {
  public static ApiError of(int status, String message) {
    return new ApiError(Instant.now(), status, message, Map.of());
  }
}
