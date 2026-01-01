package com.hospital.automation.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> reg =
                new ServletRegistrationBean<>(new JakartaWebServlet());
        reg.addUrlMappings("/h2-console/*");
        reg.addInitParameter("trace", "false");
        reg.addInitParameter("webAllowOthers", "false");
        return reg;
    }
}
