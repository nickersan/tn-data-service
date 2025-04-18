package com.tn.data.config;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.tn.data.domain.Field;
import com.tn.data.io.KeyParser;

@Configuration
class ParserConfiguration
{
  @Bean
  @Lazy
  KeyParser keyParser(Collection<Field> fields, ObjectMapper objectMapper)
  {
    return new KeyParser(fields, objectMapper);
  }
}
