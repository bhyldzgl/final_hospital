package com.hospital.automation.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class UiController {

    @GetMapping({"", "/"})
    public String home() {
        return "ui/index";
    }
}
