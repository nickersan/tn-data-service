package com.tn.service.data.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringKeyParserTest
{
  @Test
  void shouldParseKey()
  {
    String key = "ABC";
    assertEquals(key, new StringKeyParser().parse(key));
  }
}
