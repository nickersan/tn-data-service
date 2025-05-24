package com.tn.service.data.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("com.tn.service.data.controller")
public class ControllerAutoConfiguration {}