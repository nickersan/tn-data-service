package com.tn.service.data.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LongIdParserTest
{
  @Test
  void shouldParseKey()
  {
    assertEquals(2, new LongIdParser().parse("2"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "X"})
  void shouldThrowInvalidKeyException(String invalidKey)
  {
    assertThrows(InvalidIdException.class, () -> new LongIdParser().parse(invalidKey));
  }
}
