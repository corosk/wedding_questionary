package com.entertainment.controller.result;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ResultController {

    @RequestMapping(value = "/result")
    public String index(Model model) {
        return "result/result";
    }
}
