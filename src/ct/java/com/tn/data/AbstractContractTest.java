package com.tn.data;

import com.tn.data.api.NoopController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
public abstract class AbstractContractTest
{
  @Autowired
  NoopController noopController;

  @BeforeEach
  public void setup()
  {
    RestAssuredMockMvc.standaloneSetup(noopController);
  }
}
