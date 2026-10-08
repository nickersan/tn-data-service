package com.tn.service.data.io;

import java.util.Collection;
import java.util.stream.StreamSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public interface JsonCodec<T>
{
  T readValue(JsonNode jsonNode) throws JsonException;

  default Collection<T> readValues(ArrayNode arrayNode) throws JsonException
  {
    return StreamSupport.stream(arrayNode.spliterator(), false).map(this::readValue).toList();
  }

  JsonNode writeValue(Object value);

  ArrayNode writeValues(Collection<?> values);
}
