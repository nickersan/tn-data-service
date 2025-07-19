package com.tn.service.data.parameter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringIdentityParserTest
{
  @Test
  void shouldParseKey()
  {
    String key = "ABC";
    Assertions.assertEquals(key, new StringIdentityParser().parse(key));
  }
}
