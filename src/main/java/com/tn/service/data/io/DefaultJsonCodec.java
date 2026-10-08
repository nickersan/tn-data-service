package com.tn.service.data.io;

import java.util.Collection;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

public class DefaultJsonCodec<T> implements JsonCodec<T>
{
  private final ObjectMapper mapper;
  private final Class<T> type;

  public DefaultJsonCodec(ObjectMapper mapper, Class<T> type)
  {
    this.mapper = mapper;
    this.type = type;
  }

  @Override
  public T readValue(JsonNode jsonNode) throws JsonException
  {
    try
    {
      return mapper.treeToValue(jsonNode, type);
    }
    catch (JacksonException e)
    {
      throw new JsonException("Failed to read json", e);
    }
  }

  @Override
  public JsonNode writeValue(Object value)
  {
    return mapper.valueToTree(value);
  }

  @Override
  public ArrayNode writeValues(Collection<?> values)
  {
    return mapper.createArrayNode().addAll(values.stream().map(this::writeValue).toList());
  }
}
