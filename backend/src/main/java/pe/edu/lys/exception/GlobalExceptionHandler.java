package pe.edu.lys.exception;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  public record ErrorRespuesta(int estado, String mensaje, Map<String, String> campos) {}

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorRespuesta> negocio(ApiException e) {
    return ResponseEntity.status(e.getStatus())
        .body(new ErrorRespuesta(e.getStatus().value(), e.getMessage(), Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorRespuesta> validar(MethodArgumentNotValidException e) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(new ErrorRespuesta(400, "Datos inválidos", campos));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ErrorRespuesta> formato(Exception e) {
    return ResponseEntity.badRequest()
        .body(new ErrorRespuesta(400, "Formato de solicitud inválido", Map.of()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorRespuesta> conflicto(DataIntegrityViolationException e) {
    return ResponseEntity.status(409)
        .body(
            new ErrorRespuesta(
                409, "Los datos duplican o incumplen una relación existente", Map.of()));
  }

}
