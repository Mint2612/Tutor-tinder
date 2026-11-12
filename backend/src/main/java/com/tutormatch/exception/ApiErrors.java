package com.tutormatch.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiErrors {

  private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

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
    HttpMessageNotReadableException.class,
    MaxUploadSizeExceededException.class,
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
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

  @ExceptionHandler(ConcurrencyFailureException.class)
  ResponseEntity<?> concurrent(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Yêu cầu đang được cập nhật. Vui lòng thử lại."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    // Lỗi của Spring MVC (sai URL, sai method, sai Content-Type) mang sẵn mã HTTP đúng.
    if (e instanceof ErrorResponse er) {
      int status = er.getStatusCode().value();
      String message =
          switch (status) {
            case 404 -> "Không tìm thấy";
            case 405 -> "Phương thức không được hỗ trợ";
            case 415 -> "Định dạng dữ liệu không được hỗ trợ";
            default -> "Yêu cầu không hợp lệ";
          };
      return ResponseEntity.status(er.getStatusCode()).body(Map.of("message", message));
    }
    LOG.error("Unhandled API failure", e); // giữ stack trace để debug
    return ResponseEntity.internalServerError()
        .body(Map.of("message", "Có lỗi hệ thống. Vui lòng thử lại sau."));
  }
}