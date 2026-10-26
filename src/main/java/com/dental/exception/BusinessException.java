package com.dental.exception;

/** A business failure; the HTTP adapter decides how to represent it. */
public class BusinessException extends RuntimeException {
  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN
  }

  private final Kind kind;

  public BusinessException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public Kind getKind() {
    return kind;
  }

  public static BusinessException notFound(String message) {
    return new BusinessException(Kind.NOT_FOUND, message);
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(Kind.CONFLICT, message);
  }

  public static BusinessException invalid(String message) {
    return new BusinessException(Kind.INVALID_INPUT, message);
  }

  public static BusinessException unauthenticated(String message) {
    return new BusinessException(Kind.UNAUTHENTICATED, message);
  }

  public static BusinessException forbidden(String message) {
    return new BusinessException(Kind.FORBIDDEN, message);
  }
}
