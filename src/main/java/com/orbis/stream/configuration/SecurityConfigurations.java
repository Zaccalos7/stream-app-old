package com.orbis.stream.configuration;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.exceptions.FileReadingException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Configuration
public class SecurityConfigurations {


    private final LoggerMessageComponent loggerMessageComponent;

    public SecurityConfigurations( LoggerMessageComponent loggerMessageComponent) {
        this.loggerMessageComponent = loggerMessageComponent;
    }

    public String retrievesConfigurations(){
        String configurations;
        configurations = getSignKey();
        return configurations;
    }

    private String getSignKey(){
        String signKey;
        Path envPath = Path.of(".env").toAbsolutePath();

        try(Stream<String> lines = Files.lines(envPath)){
            signKey = lines
                    .findFirst()
                    .map(line-> line.split("=", 2)[1])
                    .orElseThrow(()-> new FileReadingException("file.is.empty"));

            return  signKey;
        } catch (IOException e) {
            log.error(loggerMessageComponent.printMessage("error.during.reading.key"));
            log.error(e.getMessage(), e);
            throw new FileReadingException("error.during.reading.key");
        }
    }
}
