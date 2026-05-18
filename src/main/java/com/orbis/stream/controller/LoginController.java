package com.orbis.stream.controller;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.record.LoginRecord;
import com.orbis.stream.record.RegisterRecord;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.security.AuthenticatorService;
import com.orbis.stream.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.LoginException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/")
@Tag(name = "Login", description = "API per login e registrazione")
public class LoginController {

    private final RegisterService registerService;
    private final AuthenticatorService authenticatorService;
    private final ResponseHandler responseHandler;
    private final LoggerMessageComponent loggerMessageComponent;

    @Operation(summary = "Endpoint per la registrazione, tutti i parametri sono required",
            description = "Restituisce l'esito dell'operazione")
    @PostMapping("register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRecord registerRecord) {
        ResponseEntity<Map<String, String>> mapResponseEntity;
        String message;

        registerService.register(registerRecord);

        mapResponseEntity = responseHandler.buildResponse("success.register.response", HttpStatus.CREATED);
        message = loggerMessageComponent.printMessage("success.register.response");
        log.info("{} {}", message, HttpStatus.CREATED);
        return mapResponseEntity;
    }

    @Operation(summary = "Endpoint per il login, tutti i parametri sono required",
            description = "Creazione del bearertoken per l'accesso all'app")
    @PostMapping("login")
    public String login(@Valid @RequestBody LoginRecord loginRecord) throws LoginException {
        String bearToken = authenticatorService.login(loginRecord);
        String successMessage = loggerMessageComponent.printMessage("generated.bearertoken");
        log.info(successMessage);
        return bearToken;
    }
}
