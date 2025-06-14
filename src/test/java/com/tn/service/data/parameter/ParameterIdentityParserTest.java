package com.tn.service.data.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.tn.service.IllegalParameterException;

class ParameterIdentityParserTest
{
  @Test
  void shouldParseIdentity()
  {
    String name = "id";
    String identity1 = "1";
    String identity2 = "2";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add(name, identity1);
    params.add(name, identity2);
    // including sort params to ensure they are ignored.
    params.add("$sort", "other");
    params.add("$direction", "DESCENDING");

    @SuppressWarnings("unchecked")
    IdentityParser<String, String> identityParser = mock(IdentityParser.class);
    when(identityParser.parse(identity1)).thenReturn(identity1);
    when(identityParser.parse(identity2)).thenReturn(identity2);

    assertEquals(Set.of(identity1, identity2), new ParameterIdentityParser<>(identityParser, name).parse(params));
  }

  @Test
  void shouldReturnEmptyWhenIdentityFieldsMissing()
  {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    // including sort params to ensure they are ignored.
    params.add("$sort", "other");
    params.add("$direction", "DESCENDING");

    @SuppressWarnings("unchecked")
    IdentityParser<String, String> identityParser = mock(IdentityParser.class);

    assertTrue(new ParameterIdentityParser<>(identityParser, "id").parse(params).isEmpty());
  }

  @Test
  void shouldNotParseIdentityWithOtherFields()
  {
    String name = "id";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add(name, "1");
    params.add("other", "Random other field");

    @SuppressWarnings("unchecked")
    IdentityParser<String, String> identityParser = mock(IdentityParser.class);

    assertThrows(IllegalParameterException.class, () -> new ParameterIdentityParser<>(identityParser, name).parse(params));
  }
}
