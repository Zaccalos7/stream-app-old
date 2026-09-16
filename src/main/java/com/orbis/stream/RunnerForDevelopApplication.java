package com.orbis.stream;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;


/**
 * For use this uncomment the @SpringBootApplication and comment @SpringBootApplication on StreamApplication
 * In this way you can start spring application and can run react app, without build the react app
 * every times (and copy on static folder)
 */
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class RunnerForDevelopApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunnerForDevelopApplication.class, args);
    }

}
