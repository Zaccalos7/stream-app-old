package com.orbis.stream.component;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LoggerMessageComponent {
    private final MessageSource messageSource;

    public String printMessage(String code){
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }


    public String printMessage(String code, Object[] params){
        return messageSource.getMessage(code , params, LocaleContextHolder.getLocale());
    }
}
