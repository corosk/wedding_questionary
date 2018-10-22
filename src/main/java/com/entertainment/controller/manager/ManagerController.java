package com.entertainment.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ManagerController {

    @RequestMapping(value = "/manager")
    public String index(Model model) {
        return "manager/manager";
    }
}
