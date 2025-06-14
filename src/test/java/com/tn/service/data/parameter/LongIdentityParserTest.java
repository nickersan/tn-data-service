package com.tn.service.data.parameter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LongIdentityParserTest
{
  @Test
  void shouldParseKey()
  {
    Assertions.assertEquals(2, new LongIdentityParser().parse("2"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "X"})
  void shouldThrowInvalidKeyException(String invalidKey)
  {
    assertThrows(InvalidIdException.class, () -> new LongIdentityParser().parse(invalidKey));
  }
}
