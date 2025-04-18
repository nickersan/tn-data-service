package com.tn.data;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import com.tn.data.controller.NoopController;
import com.tn.data.repository.DataRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
public abstract class AbstractContractTest
{
  @Autowired
  NoopController noopController;

//  @Autowired
//  DataController dataController;

  @MockBean
  DataRepository dataRepository;

  @BeforeEach
  public void setup()
  {
    RestAssuredMockMvc.standaloneSetup(noopController);
  }
}
