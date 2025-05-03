package com.tn.data.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("com.tn.data.controller")
public class ControllerAutoConfiguration {}