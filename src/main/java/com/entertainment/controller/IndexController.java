package com.entertainment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * TOP画面のコントローラー
 * URL("/")
 *
 * @author k_mikami
 */
@Controller
public class IndexController {
    @RequestMapping(value = "/")
    public String index(Model model) {
        return "index";
    }
}
