package com.cscd488seniorproject.syllabussyncproject.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class SpaController {

    //This forwards all non "/api" requests to index.html so that the vue router can handle the routing on the frontend
    @RequestMapping(value = { "/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
   }
   
}
