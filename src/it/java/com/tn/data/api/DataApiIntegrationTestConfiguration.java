package com.tn.data.api;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;

@Configuration
@Profile("api-integration-test")
class DataApiIntegrationTestConfiguration
{
  @Bean
  DataRepository dataRepository()
  {
    return mock(DataRepository.class);
  }

  @Bean
  KeyParser keyParser()
  {
    return mock(KeyParser.class);
  }
}
