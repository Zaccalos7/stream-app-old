package com.orbis.stream.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ResponseHandler {
    private final MessageSource messageSource;

    public ResponseEntity<Map<String, String>> build(String code, HttpStatus status, Object[] params) {
        Map<String, String> response = new HashMap<>();
        String localizedMessage = messageSource.getMessage(code, params != null ? params : new Object[]{}, LocaleContextHolder.getLocale());

        String outcome = status.is2xxSuccessful() ? "success" : "error";

        response.put("response", outcome);
        response.put("message", localizedMessage);

        return new ResponseEntity<>(response, status);
    }

    public ResponseEntity<Map<String, String>> buildBadResponse(String code, HttpStatus status, Object[] params) {
        return build(code, status, params);
    }

    public ResponseEntity<Map<String, String>> buildBadResponse(String code, HttpStatus status) {
        return build(code, status, null);
    }

    public ResponseEntity<Map<String, String>> buildBadResponse(String code, Object[] params) {
        return build(code, HttpStatus.INTERNAL_SERVER_ERROR, params);
    }

    public ResponseEntity<Map<String, String>> buildBadResponse(String code) {
        return build(code, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }


    /*
        This method is useful when you’re dealing with an exception whose message can vary in many different ways
     */
    public ResponseEntity<Map<String, String>> buildBadResponseWithoutMessageLabel(String message){
        Map<String, String> response = new HashMap<>();
        response.put("response", "error");
        response.put("message", message);

        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
