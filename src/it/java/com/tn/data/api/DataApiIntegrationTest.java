package com.tn.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import static com.tn.data.controller.DataController.DEFAULT_PAGE_NUMBER;
import static com.tn.data.controller.DataController.DEFAULT_PAGE_SIZE;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;
import com.tn.lang.util.Page;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api-integration-test")
class DataApiIntegrationTest
{
  private static final String FIELD_ID = "id";
  private static final String FIELD_ID_2 = "id2";
  private static final String FIELD_NAME = "name";
  private static final ParameterizedTypeReference<Page<ObjectNode>> TYPE_REFERENCE_PAGE = new ParameterizedTypeReference<>() {};

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
  void shouldGet()
  {
    ObjectNode data1 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    ObjectNode data2 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(2), FIELD_NAME, TextNode.valueOf("Data 2")));

    when(dataRepository.findAll()).thenReturn(List.of(data1, data2));

    ResponseEntity<ArrayNode> response = testRestTemplate.getForEntity("/", ArrayNode.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(new ArrayNode(null, List.of(data1, data2)), response.getBody());
  }

  @Test
  void shouldGetWithSimpleKeys()
  {
    ObjectNode key1 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1)));
    ObjectNode key2 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(2)));
    ObjectNode data1 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    ObjectNode data2 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(2), FIELD_NAME, TextNode.valueOf("Data 2")));

    when(keyParser.parse(key1.get(FIELD_ID).asText())).thenReturn(key1);
    when(keyParser.parse(key2.get(FIELD_ID).asText())).thenReturn(key2);
    when(dataRepository.findAll(List.of(key1, key2))).thenReturn(List.of(data1, data2));

    String url = UriComponentsBuilder.fromPath("/")
      .queryParam("key", List.of(key1.get(FIELD_ID).asText(), key2.get(FIELD_ID).asText()))
      .encode()
      .toUriString();

    ResponseEntity<ArrayNode> response = testRestTemplate.getForEntity(url, ArrayNode.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(new ArrayNode(null, List.of(data1, data2)), response.getBody());
  }

  @Test
  void shouldGetWithComplexKey() throws Exception
  {
    ObjectNode key = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_ID_2, TextNode.valueOf("A")));
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_ID_2, TextNode.valueOf("A"), FIELD_NAME, TextNode.valueOf("Data 1")));

    String encodedKey = encode(key);

    when(keyParser.parse(encodedKey)).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.of(data));

    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/" + encodedKey, ObjectNode.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(data, response.getBody());
  }

  @Test
  void shouldGetWithComplexKeys() throws Exception
  {
    ObjectNode key1 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_ID_2, TextNode.valueOf("A")));
    ObjectNode key2 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(2), FIELD_ID_2, TextNode.valueOf("B")));
    ObjectNode data1 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_ID_2, TextNode.valueOf("A"), FIELD_NAME, TextNode.valueOf("Data 1")));
    ObjectNode data2 = objectNode(Map.of(FIELD_ID, IntNode.valueOf(2), FIELD_ID_2, TextNode.valueOf("B"), FIELD_NAME, TextNode.valueOf("Data 2")));

    String encodedKey1 = encode(key1);
    String encodedKey2 = encode(key2);

    when(keyParser.parse(encodedKey1)).thenReturn(key1);
    when(keyParser.parse(encodedKey2)).thenReturn(key2);
    when(dataRepository.findAll(List.of(key1, key2))).thenReturn(List.of(data1, data2));

    String url = UriComponentsBuilder.fromPath("/")
      .queryParam("key", List.of(encodedKey1, encodedKey2))
      .encode()
      .toUriString();

    ResponseEntity<ArrayNode> response = testRestTemplate.getForEntity(url, ArrayNode.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(new ArrayNode(null, List.of(data1, data2)), response.getBody());
  }

  @Test
  void shouldGetWithQuery()
  {
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    String query = "name=Data 1";

    when(dataRepository.findFor(query)).thenReturn(List.of(data));

    ResponseEntity<ArrayNode> response = testRestTemplate.getForEntity("/?q=" + query, ArrayNode.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(new ArrayNode(null, List.of(data)), response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumber()
  {
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    String query = "name=Data 1";
    int pageNumber = 1;

    Page<ObjectNode> page = new Page<>(List.of(data), pageNumber, 2, DEFAULT_PAGE_SIZE + 1);

    when(dataRepository.findFor(query, pageNumber, DEFAULT_PAGE_SIZE)).thenReturn(page);

    ResponseEntity<Page<ObjectNode>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageNumber=" + pageNumber,
      HttpMethod.GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageSize()
  {
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    String query = "name=Data 1";
    int pageSize = 10;

    Page<ObjectNode> page = new Page<>(List.of(data), DEFAULT_PAGE_NUMBER, 2, pageSize + 1);

    when(dataRepository.findFor(query, DEFAULT_PAGE_NUMBER, pageSize)).thenReturn(page);

    ResponseEntity<Page<ObjectNode>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageSize=" + pageSize,
      HttpMethod.GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumberAndPageSize()
  {
    ObjectNode data = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1), FIELD_NAME, TextNode.valueOf("Data 1")));
    String query = "name=Data 1";
    int pageNumber = 1;
    int pageSize = 10;

    Page<ObjectNode> page = new Page<>(List.of(data), pageNumber, 2, pageSize + 1);

    when(dataRepository.findFor(query, pageNumber, pageSize)).thenReturn(page);

    ResponseEntity<Page<ObjectNode>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageNumber=" + pageNumber + "&pageSize=" + pageSize,
      HttpMethod.GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldNotGetForUnknownKey()
  {
    ObjectNode key = objectNode(Map.of(FIELD_ID, IntNode.valueOf(1)));

    when(keyParser.parse(key.get(FIELD_ID).asText())).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.empty());

    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/" + key.get(FIELD_ID).asText(), ObjectNode.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldNotGetWithKeyAndQuery()
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/?key=1&q=x", ObjectNode.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
    assertNotNull(response.getBody());
    assertEquals("Query parameter not allowed with key(s)", response.getBody().get("message").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"pageNumber=0", "pageSize=10", "pageNumber=0&pageSize=10"})
  void shouldNotGetWithKeyAndPagination(String paginationParameter)
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/?key=1&" + paginationParameter, ObjectNode.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
    assertNotNull(response.getBody());
    assertEquals("Pagination not supported with Key(s)", response.getBody().get("message").asText());
  }

  private ObjectNode objectNode(Map<String, JsonNode> properties)
  {
    ObjectNode objectNode = new ObjectNode(null);
    objectNode.setAll(properties);

    return objectNode;
  }

  private String encode(ObjectNode object) throws IOException
  {
    return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(object));
  }
}
