package com.tn.service.data.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DefaultJsonCodecTest
{
  private static final JsonCodec<Subject> JSON_CODEC = new DefaultJsonCodec<>(new ObjectMapper(), Subject.class);

  @Test
  void shouldHandleObject()
  {
    Subject expectSubject = new Subject(1, "ONE");
    Subject deserializeSubject = JSON_CODEC.readValue(JSON_CODEC.writeValue(expectSubject));

    assertEquals(expectSubject, deserializeSubject);
    assertNotSame(expectSubject, deserializeSubject);
  }

  @Test
  void shouldHandleList()
  {
    List<Subject> expectSubjects = List.of(new Subject(1, "ONE"), new Subject(2, "TWO"), new Subject(3, "THREE"));

    assertEquals(expectSubjects, JSON_CODEC.readValues(JSON_CODEC.writeValues(expectSubjects)));
  }

  record Subject(int id, String name) {}
}
