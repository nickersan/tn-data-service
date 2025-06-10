package com.tn.service.data.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringIdParserTest
{
  @Test
  void shouldParseKey()
  {
    String key = "ABC";
    assertEquals(key, new StringIdParser().parse(key));
  }
}
