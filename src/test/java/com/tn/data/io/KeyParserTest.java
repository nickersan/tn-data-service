package com.tn.data.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.tn.data.domain.Field;
import com.tn.data.domain.FieldType;

class KeyParserTest
{
  private static final String FIELD_NAME = "id";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ParameterizedTest
  @MethodSource("singleFieldKeys")
  void shouldParseSingleFieldKey(Field field, String key, ObjectNode expected)
  {
    assertEquals(expected, new KeyParser(List.of(field), OBJECT_MAPPER).parse(key));
  }

  private static Stream<Arguments> singleFieldKeys()
  {
    LocalDateTime now = LocalDateTime.now();

    return Stream.of(
      Arguments.of(FieldType.BOOLEAN.field(FIELD_NAME, null), "true", objectNode(BooleanNode.valueOf(true))),
      Arguments.of(FieldType.INTEGER.field(FIELD_NAME, null), "12", objectNode(IntNode.valueOf(12))),
      Arguments.of(FieldType.LONG.field(FIELD_NAME, null), "12", objectNode(LongNode.valueOf(12))),
      Arguments.of(FieldType.FLOAT.field(FIELD_NAME, null), "1.23", objectNode(FloatNode.valueOf(1.23F))),
      Arguments.of(FieldType.DOUBLE.field(FIELD_NAME, null), "1.23", objectNode(DoubleNode.valueOf(1.23))),
      Arguments.of(FieldType.DECIMAL.field(FIELD_NAME, null), "1.23", objectNode(DecimalNode.valueOf(BigDecimal.valueOf(1.23)))),
      Arguments.of(FieldType.TEXT.field(FIELD_NAME, null), "ABC", objectNode(TextNode.valueOf("ABC"))),
      Arguments.of(FieldType.DATE.field(FIELD_NAME, null), now.toLocalDate().toString(), objectNode(TextNode.valueOf(now.toLocalDate().toString()))),
      Arguments.of(FieldType.TIME.field(FIELD_NAME, null), now.toLocalTime().toString(), objectNode(TextNode.valueOf(now.toLocalTime().toString()))),
      Arguments.of(FieldType.TIMESTAMP.field(FIELD_NAME, null), now.toString(), objectNode(TextNode.valueOf(now.toString())))
    );
  }

  @Test
  void shouldParseObjectKey()
  {

  }

  private static Object objectNode(JsonNode value)
  {
    ObjectNode objectNode = new ObjectNode(null);
    objectNode.set(FIELD_NAME, value);

    return objectNode;
  }
}
