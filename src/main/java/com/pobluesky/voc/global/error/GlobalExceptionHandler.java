package com.pobluesky.voc.global.error;

import com.pobluesky.voc.global.util.ResponseFactory;
import com.pobluesky.voc.global.util.model.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResult> defaultException(Exception e) {
        log.error(e.getMessage());
        log.error(String.valueOf(e));

        return new ResponseEntity<>(
            ResponseFactory.getFailResult(
                ErrorCode.UNKNOWN.getCode(),
                ErrorCode.UNKNOWN.getMessage()
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /*
    서버 단에서 예상되는 예외 처리
     */
    @ExceptionHandler(CommonException.class)
    protected ResponseEntity<CommonResult> handleCustomException(CommonException e) {
        log.error(e.getErrorCode().getMessage());


        ErrorCode errorCode = e.getErrorCode();

        return new ResponseEntity<>(
            ResponseFactory.getFailResult(
                errorCode.getCode(),
                errorCode.getMessage()
            ),
            errorCode.getStatus()
        );
    }

    /*
    잘못된 요청에 대한 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResult> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.error(e.getMessage());

        BindingResult bindingResult = e.getBindingResult();

        String message = bindingResult.getFieldError().getDefaultMessage();

        return new ResponseEntity<>(
            ResponseFactory.getFailResult(
                ErrorCode.INVALID_REQUEST.getCode(),
                message),
            HttpStatus.BAD_REQUEST
        );
    }
}
