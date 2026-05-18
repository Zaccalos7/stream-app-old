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

    public ResponseEntity<Map<String, String>> buildResponse(String code, HttpStatus status){
        Map<String , String> response = new HashMap<>();

        String localizedMessage = messageSource.getMessage(code, new Object[]{}, LocaleContextHolder.getLocale());
        response.put("response", "success");
        response.put("message", localizedMessage);

        return new ResponseEntity<>(response, status);
    }

    public Map<String, String> buildBadResponse(String code){
        Map<String , String> response = new HashMap<>();

        String localizedMessage = messageSource.getMessage(code, new Object[]{}, LocaleContextHolder.getLocale());
        response.put("response", "error");
        response.put("message", localizedMessage);

        return response;
    }

    public ResponseEntity<Map<String, String>> buildBadResponse(String code, HttpStatus status){
        Map<String , String> response = new HashMap<>();

        String localizedMessage = messageSource.getMessage(code, new Object[]{}, LocaleContextHolder.getLocale());
        response.put("response", "error");
        response.put("message", localizedMessage);

        return new ResponseEntity<>(response, status);
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
