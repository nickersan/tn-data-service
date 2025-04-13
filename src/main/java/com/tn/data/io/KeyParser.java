package com.tn.data.io;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.tn.data.domain.Field;

public class KeyParser
{
  private final Collection<Field> keyFields;
  private final ObjectMapper objectMapper;

  public KeyParser(Collection<Field> keyFields, ObjectMapper objectMapper)
  {
    this.keyFields = keyFields;
    this.objectMapper = objectMapper;
  }

  public ObjectNode parse(String key) throws InvalidKeyException
  {
    return keyFields.size() > 1 ? parseAsObject(key) : parseAsValue(key);
  }

  private ObjectNode parseAsObject(String key)
  {
    try
    {
      return checkFields(objectMapper.readValue(Base64.getDecoder().decode(key), ObjectNode.class));
    }
    catch (IOException e)
    {
      throw new InvalidKeyException("Invalid key: " + key, e);
    }
  }

  private ObjectNode parseAsValue(String key)
  {
    try
    {
      if (keyFields.size() > 1) throw new InvalidKeyException("Invalid key: " + key);
      Field keyField = keyFields.stream().findFirst().orElseThrow(() -> new InvalidKeyException("Invalid key: " + key));

      return objectNode(keyField, key);
    }
    catch (NumberFormatException | DateTimeParseException e)
    {
      throw new KeyParseException("Cannot parse key: " + key, e);
    }
  }

  private ObjectNode checkFields(ObjectNode key)
  {
    keyFields.forEach(coerceFieldType(key));
    return key;
  }

  private Consumer<Field> coerceFieldType(ObjectNode key)
  {
    return field ->
    {
      if (!field.existsIn(key))
      {
        JsonNode value = key.get(field.name());
        if (value == null) throw new InvalidKeyException("Invalid key: " + key + " - field: " + field.name());

        try
        {
          key.set(field.name(), field.coerce(value));
        }
        catch (IllegalArgumentException e)
        {
          throw new InvalidKeyException("Invalid key: " + key + " - field: " + field.name(), e);
        }
      }
    };
  }

  private ObjectNode objectNode(Field field, String key)
  {
    ObjectNode objectNode = new ObjectNode(null);
    objectNode.set(field.name(), field.parseAsJsonType(key));
    
    return objectNode;
  }
}
