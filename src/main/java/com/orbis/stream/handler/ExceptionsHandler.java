package com.orbis.stream.handler;


import com.orbis.stream.exceptions.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionsHandler {

    private final MessageSource messageSource;
    private final ResponseHandler responseHandler;

    public ExceptionsHandler(MessageSource messageSource, ResponseHandler responseHandler) {
        this.messageSource = messageSource;
        this.responseHandler = responseHandler;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleException(MethodArgumentNotValidException ex){
        Map<String, String> errorResponse = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        {
                            String code = fieldError.getDefaultMessage();
                            String message = messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
                            errorResponse.put(fieldError.getField(), message);
                        }

                );

        return  ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> handleException(SQLException ex){
        String errorMessage = ex.getLocalizedMessage();
        return responseHandler.buildBadResponseWithoutMessageLabel(errorMessage);
    }

    @ExceptionHandler(FileReadingException.class)
    public ResponseEntity<Map<String, String>> handleException(FileReadingException ex){
        Map<String, String> errorResponse;
        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage);
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<Map<String, String>> handleException(LoginException ex){
        Map<String, String> errorResponse;
        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage);
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(DuplicationEntityException.class)
    public ResponseEntity<Map<String, String>> handleException(DuplicationEntityException ex){
        ResponseEntity<Map<String, String>> errorResponse;
        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage, HttpStatus.CONFLICT);
        return errorResponse;
    }

    @ExceptionHandler(NotFoundCustomException.class)
    public ResponseEntity<Map<String, String>> handleException(NotFoundCustomException ex){
        ResponseEntity<Map<String, String>> errorResponse;

        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage, HttpStatus.NOT_FOUND);
        return errorResponse;
    }


    @ExceptionHandler(StreamingException.class)
    public ResponseEntity<Map<String, String>> handleException(StreamingException ex){
        ResponseEntity<Map<String, String>> errorResponse;

        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        return errorResponse;
    }

    @ExceptionHandler(LiveException.class)
    public ResponseEntity<Map<String, String>> handleException(LiveException ex){
        ResponseEntity<Map<String, String>> errorResponse;

        String errorCodeMessage = ex.getLocalizedMessage();
        errorResponse = responseHandler.buildBadResponse(errorCodeMessage, HttpStatus.CONFLICT);
        return errorResponse;
    }

}
