package com.tn.data.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class NoopApiIntegrationTest
{
  @Autowired
  TestRestTemplate testRestTemplate;

  @Test
  void shouldGetNoop()
  {
    assertTrue(testRestTemplate.getForEntity("/noop", Void.class).getStatusCode().is2xxSuccessful());
  }
}
