package com.tn.data.io;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import com.tn.data.domain.Field;

@RequiredArgsConstructor
public class KeyParser
{
  private final Collection<Field> keyFields;
  private final ObjectMapper objectMapper;

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
    keyFields.forEach(keyField -> { if (!key.has(keyField.name())) throw new InvalidKeyException("Invalid key: " + key); });
    return key;
  }

  private ObjectNode objectNode(Field field, Object value)
  {
    ObjectNode objectNode = new ObjectNode(null);
    objectNode.set(field.name(), field.type().asJsonType(value));
    
    return objectNode;
  }
}
