package com.tutormatch.exception;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<?> business(BusinessException e) {
    return ResponseEntity.status(e.status()).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> known(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("message", e.getReason() == null ? "Yêu cầu không hợp lệ" : e.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.multipart.MaxUploadSizeExceededException.class,
    jakarta.validation.ConstraintViolationException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường."));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Dữ liệu đã tồn tại hoặc yêu cầu đang chờ xử lý."));
  }

  @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
  ResponseEntity<?> concurrent(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Yêu cầu đang được cập nhật. Vui lòng thử lại."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .error("Unhandled API failure: {}", e.getClass().getName());
    return ResponseEntity.internalServerError()
        .body(Map.of("message", "Có lỗi hệ thống. Vui lòng thử lại sau."));
  }
}
