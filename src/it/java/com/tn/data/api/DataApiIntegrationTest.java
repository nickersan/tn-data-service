package com.tn.data.api;

import static java.lang.String.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import static com.tn.data.controller.DataController.DEFAULT_PAGE_NUMBER;
import static com.tn.data.controller.DataController.DEFAULT_PAGE_SIZE;
import static com.tn.data.controller.DataController.FIELD_MESSAGE;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import com.tn.data.autoconfig.ControllerAutoConfiguration;
import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;
import com.tn.data.repository.DeleteException;
import com.tn.data.repository.InsertException;
import com.tn.data.repository.UpdateException;
import com.tn.lang.util.Page;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {DataApiIntegrationTest.TestConfiguration.class, ControllerAutoConfiguration.class},
  properties = "tn.data.value-class=com.tn.data.api.DataApiIntegrationTest.Value"
)
@SuppressWarnings("SpringBootApplicationProperties")
@EnableAutoConfiguration
class DataApiIntegrationTest
{
  private static final ParameterizedTypeReference<List<Value>> TYPE_REFERENCE_LIST = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<Page<Value>> TYPE_REFERENCE_PAGE = new ParameterizedTypeReference<>() {};

  @Autowired
  TestRestTemplate testRestTemplate;

  @Autowired
  DataRepository<Integer, Value> dataRepository;

  @Autowired
  KeyParser<Integer> keyParser;

  @BeforeEach
  void resetMocks()
  {
    reset(dataRepository, keyParser);
  }

  @Test
  void shouldGet()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(dataRepository.findAll()).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange("/", GET, null, TYPE_REFERENCE_LIST);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithKey()
  {
    Value value = new Value(123, "TEST");

    when(keyParser.parse(value.id().toString())).thenReturn(value.id());
    when(dataRepository.find(value.id())).thenReturn(Optional.of(value));

    ResponseEntity<Value> response = testRestTemplate.getForEntity("/" + value.id(), Value.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(value, response.getBody());
  }

  @Test
  void shouldGetWithKeys()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(keyParser.parse(value1.id().toString())).thenReturn(value1.id());
    when(keyParser.parse(value2.id().toString())).thenReturn(value2.id());
    when(dataRepository.findAll(List.of(value1.id(), value2.id()))).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?key=%s&key=%s", value1.id(), value2.id()),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithQuery()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();

    when(dataRepository.findFor(query)).thenReturn(List.of(value));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      "/?q=" + query,
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value), response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumber()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();
    int pageNumber = 1;

    Page<Value> page = new Page<>(List.of(value), pageNumber, 2, DEFAULT_PAGE_SIZE + 1);

    when(dataRepository.findFor(query, pageNumber, DEFAULT_PAGE_SIZE)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageNumber=" + pageNumber,
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageSize()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();
    int pageSize = 10;

    Page<Value> page = new Page<>(List.of(value), DEFAULT_PAGE_NUMBER, 2, pageSize + 1);

    when(dataRepository.findFor(query, DEFAULT_PAGE_NUMBER, pageSize)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageSize=" + pageSize,
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumberAndPageSize()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" +  value.name();
    int pageNumber = 1;
    int pageSize = 10;

    Page<Value> page = new Page<>(List.of(value), pageNumber, 2, pageSize + 1);

    when(dataRepository.findFor(query, pageNumber, pageSize)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      "/?q=" + query + "&pageNumber=" + pageNumber + "&pageSize=" + pageSize,
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldNotGetForUnknownKey()
  {
    Integer key = 123;

    when(keyParser.parse(key.toString())).thenReturn(key);
    when(dataRepository.find(key)).thenReturn(Optional.empty());

    ResponseEntity<Value> response = testRestTemplate.getForEntity("/" + key, Value.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldNotGetWithKeyAndQuery()
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/?key=1&q=x", ObjectNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Query parameter not allowed with key(s)", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"pageNumber=0", "pageSize=10", "pageNumber=0&pageSize=10"})
  void shouldNotGetWithKeyAndPagination(String paginationParameter)
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/?key=1&" + paginationParameter, ObjectNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Pagination not supported with Key(s)", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @Test
  void shouldPostWithObject()
  {
    Value value = new Value(123, "TEST");

    when(dataRepository.insert(value)).thenReturn(value);

    ResponseEntity<Value> response = testRestTemplate.postForEntity("/", value, Value.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(value, response.getBody());

    verify(dataRepository).insert(value);
  }

  @Test
  void shouldPostWithArray()
  {
    List<Value> values = List.of(new Value(1, "ONE"), new Value(2, "TWO"));

    when(dataRepository.insertAll(values)).thenReturn(values);

    ResponseEntity<List<Value>> response = testRestTemplate.exchange("/", POST, body(values), TYPE_REFERENCE_LIST);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(values, response.getBody());

    verify(dataRepository).insertAll(values);
  }

  @Test
  void shouldNotPostWithInvalidBody()
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.postForEntity("/", List.of("INVALID"), ObjectNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Invalid body", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @Test
  void shouldNotPostWithRepositoryError()
  {
    Value value = new Value(123, "TEST");

    when(dataRepository.insert(value)).thenThrow(new InsertException("TESTING"));

    ResponseEntity<ObjectNode> response = testRestTemplate.postForEntity("/", value, ObjectNode.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("TESTING", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @Test
  void shouldPutWithObject()
  {
    Value value = new Value(123, "TEST");

    when(dataRepository.update(value)).thenReturn(value);

    ResponseEntity<Value> response = testRestTemplate.exchange("/", PUT, body(value), Value.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(value, response.getBody());

    verify(dataRepository).update(value);
  }

  @Test
  void shouldPutWithArray()
  {
    List<Value> values = List.of(new Value(1, "ONE"), new Value(2, "TWO"));

    when(dataRepository.updateAll(values)).thenReturn(values);

    ResponseEntity<List<Value>> response = testRestTemplate.exchange("/", PUT, body(values), TYPE_REFERENCE_LIST);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(values, response.getBody());

    verify(dataRepository).updateAll(values);
  }

  @Test
  void shouldNotPutWithInvalidBody()
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.exchange("/", PUT, body(List.of("INVALID")), ObjectNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Invalid body", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @Test
  void shouldNotPutWithRepositoryError()
  {
    Value value = new Value(123, "TEST");

    when(dataRepository.update(value)).thenThrow(new UpdateException("TESTING"));

    ResponseEntity<ObjectNode> response = testRestTemplate.exchange("/", PUT, body(value), ObjectNode.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("TESTING", response.getBody().get(FIELD_MESSAGE).asText());
  }

  @Test
  void shouldDeleteWithKey()
  {
    int key = 123;

    when(keyParser.parse(Integer.toString(key))).thenReturn(key);

    ResponseEntity<Void> response = testRestTemplate.exchange("/" + key, DELETE, null, Void.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(dataRepository).delete(key);
  }

  @Test
  void shouldDeleteWithKeys()
  {
    int key1 = 123;
    int key2 = 234;

    when(keyParser.parse(Integer.toString(key1))).thenReturn(key1);
    when(keyParser.parse(Integer.toString(key2))).thenReturn(key2);

    ResponseEntity<Void> response = testRestTemplate.exchange(
      format("/?key=%s&key=%s", key1, key2),
      DELETE,
      null,
      Void.class
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(dataRepository).deleteAll(List.of(key1, key2));
  }

  @Test
  void shouldNotDeleteWithRepositoryError()
  {
    int key = 123;

    when(keyParser.parse(Integer.toString(key))).thenReturn(key);
    doThrow(new DeleteException("TESTING")).when(dataRepository).delete(key);

    ResponseEntity<ObjectNode> response = testRestTemplate.exchange("/" + key, DELETE, null, ObjectNode.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("TESTING", response.getBody().get(FIELD_MESSAGE).asText());
  }

  private HttpEntity<?> body(Object body)
  {
    return new HttpEntity<>(body, new LinkedMultiValueMap<>());
  }

  public record Value(Integer id, String name) {}

  @Configuration
  static class TestConfiguration
  {
    @Bean
    DataRepository<Integer, Value> dataRepository()
    {
      //noinspection unchecked
      return mock(DataRepository.class);
    }

    @Bean
    KeyParser<Integer> keyParser()
    {
      //noinspection unchecked
      return mock(KeyParser.class);
    }
  }
}

