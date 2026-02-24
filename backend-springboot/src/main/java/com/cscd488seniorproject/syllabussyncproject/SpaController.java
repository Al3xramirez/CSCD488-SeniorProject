package com.cscd488seniorproject.syllabussyncproject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class SpaController {

    // This fowards all non "/api" requests to index.html so that the vue router can handle the routing on the frontend
    @RequestMapping(value = {"/{path:^(?!api).*$}", "/**/{path:^(?!api).*$}"})
    public String foward() {
        return "forward:/index.html";
    }
}
