package com.tn.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-integration-test")
class DataApiIntegrationTest
{
  private static final String FIELD_ID = "id";
  private static final String FIELD_ID_2 = "id2";
  private static final String FIELD_NAME = "name";

  @Autowired
  TestRestTemplate testRestTemplate;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  DataRepository dataRepository;

  @Autowired
  KeyParser keyParser;

  @Test
  void shouldGetWithSimpleKey()
  {
    ObjectNode key = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1)));
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));

    when(keyParser.parse(key.get(FIELD_ID).asText())).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.of(data));

    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/" + key.get(FIELD_ID).asText(), ObjectNode.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(data, response.getBody());
  }

  @Test
  void shouldGetWithComplexKey() throws Exception
  {
    ObjectNode key = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_ID_2, TextNode.valueOf("B")));
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));

    String encodedKey = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(key));

    when(keyParser.parse(encodedKey)).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.of(data));

    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/" + encodedKey, ObjectNode.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(data, response.getBody());
  }

  @Test
  void shouldGetNotFound()
  {
    ObjectNode key = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1)));

    when(keyParser.parse(key.get(FIELD_ID).asText())).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.empty());

    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/" + key.get(FIELD_ID).asText(), ObjectNode.class);
    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND));
  }

  private ObjectNode objectNode(Map<String, JsonNode> properties)
  {
    ObjectNode objectNode = new ObjectNode(null);
    objectNode.setAll(properties);

    return objectNode;
  }
}
