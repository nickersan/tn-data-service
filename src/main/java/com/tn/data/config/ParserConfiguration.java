package com.tn.data.config;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.tn.data.domain.Field;
import com.tn.data.io.Base64KeyParser;
import com.tn.data.io.KeyParser;

@Configuration
@Profile("!api-integration-test")
class ParserConfiguration
{
  @Bean
  @Lazy
  KeyParser keyParser(Collection<Field> fields, ObjectMapper objectMapper)
  {
    return new Base64KeyParser(fields, objectMapper);
  }
}
