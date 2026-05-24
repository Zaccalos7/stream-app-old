package com.orbis.stream.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ReactController {

    @GetMapping(value = {
            "/{path:[^\\.]*}",
            "/**/{path:[^\\.]*}"
    })
    public String forward(HttpServletRequest request) {
        if ("/error".equals(request.getRequestURI())) {
            return "forward:/error";
        }
        return "forward:/index.html";
    }
}
