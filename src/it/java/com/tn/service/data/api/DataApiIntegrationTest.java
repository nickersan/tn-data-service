package com.tn.service.data.api;

import static java.lang.String.format;
import static java.util.Collections.emptySet;

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

import static com.tn.service.data.controller.DataController.DEFAULT_PAGE_NUMBER;
import static com.tn.service.data.controller.DataController.DEFAULT_PAGE_SIZE;
import static com.tn.service.data.controller.DataController.FIELD_MESSAGE;
import static com.tn.service.data.domain.Direction.ASCENDING;
import static com.tn.service.data.domain.Direction.DESCENDING;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import com.tn.lang.util.Page;
import com.tn.service.data.domain.Direction;
import com.tn.service.data.io.DefaultJsonCodec;
import com.tn.service.data.io.JsonCodec;
import com.tn.service.data.parameter.IdentityParser;
import com.tn.service.data.parameter.QueryBuilder;
import com.tn.service.data.repository.DataRepository;
import com.tn.service.data.repository.DeleteException;
import com.tn.service.data.repository.InsertException;
import com.tn.service.data.repository.UpdateException;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = DataApiIntegrationTest.TestConfiguration.class
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
  IdentityParser<Integer> identityParser;

  @BeforeEach
  void resetMocks()
  {
    reset(dataRepository, identityParser);
  }

  @Test
  void shouldGet()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(dataRepository.findAll(emptySet(), ASCENDING)).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange("/", GET, null, TYPE_REFERENCE_LIST);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithSort()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    String sort = "name";
    Direction direction = DESCENDING;

    when(dataRepository.findAll(Set.of(sort), direction)).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?$sort=%s&$direction=%s", sort, direction),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithId()
  {
    Value value = new Value(123, "TEST");

    when(identityParser.parse(value.id().toString())).thenReturn(value.id());
    when(dataRepository.find(value.id())).thenReturn(Optional.of(value));

    ResponseEntity<Value> response = testRestTemplate.getForEntity("/" + value.id(), Value.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(value, response.getBody());
  }
  
  @Test
  void shouldGetWithIds()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(identityParser.parse(value1.id().toString())).thenReturn(value1.id());
    when(identityParser.parse(value2.id().toString())).thenReturn(value2.id());
    when(dataRepository.findWhere(format("(id=%s||id=%s)", value1.id(), value2.id()), emptySet(), ASCENDING)).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?id=%s&id=%s", value1.id(), value2.id()),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetEmptyWithIds()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(identityParser.parse(value1.id().toString())).thenReturn(value1.id());
    when(identityParser.parse(value2.id().toString())).thenReturn(value2.id());

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?id=%s&id=%s", value1.id(), value2.id()),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
  }

  @Test
  void shouldGetWithIdsAndSort()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    String sort = "name";

    when(identityParser.parse(value1.id().toString())).thenReturn(value1.id());
    when(identityParser.parse(value2.id().toString())).thenReturn(value2.id());
    when(dataRepository.findWhere(format("(id=%s||id=%s)", value1.id(), value2.id()), Set.of(sort), ASCENDING)).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?id=%s&id=%s&$sort=%s", value1.id(), value2.id(), sort),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithIdsAndDirection()
  {
    Value value1 = new Value(1, "ONE");
    Value value2 = new Value(2, "TWO");

    when(identityParser.parse(value1.id().toString())).thenReturn(value1.id());
    when(identityParser.parse(value2.id().toString())).thenReturn(value2.id());
    when(dataRepository.findWhere(format("(id=%s||id=%s)", value1.id(), value2.id()), emptySet(), DESCENDING)).thenReturn(List.of(value1, value2));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?id=%s&id=%s&$direction=DESCENDING", value1.id(), value2.id()),
      GET,
      null,
      TYPE_REFERENCE_LIST
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertNotNull(response.getBody());
    assertEquals(List.of(value1, value2), response.getBody());
  }

  @Test
  void shouldGetWithQuery()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();

    when(dataRepository.findWhere(query, emptySet(), ASCENDING)).thenReturn(List.of(value));

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
  void shouldGetWithQueryAndSort()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();

    String sort = "name";
    Direction direction = DESCENDING;

    when(dataRepository.findWhere(query, Set.of(sort), direction)).thenReturn(List.of(value));

    ResponseEntity<List<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$sort=%s&$direction=%s", query, sort, direction),
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

    Page<Value> page = new Page<>(List.of(value), pageNumber, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE + 1, 2);

    when(dataRepository.findWhere(query, pageNumber, DEFAULT_PAGE_SIZE, emptySet(), ASCENDING)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageNumber=%d", query, pageNumber),
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumberAndSort()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();
    int pageNumber = 1;

    String sort = "name";
    Direction direction = DESCENDING;

    Page<Value> page = new Page<>(List.of(value), pageNumber, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE + 1, 2);

    when(dataRepository.findWhere(query, pageNumber, DEFAULT_PAGE_SIZE, Set.of(sort), direction)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageNumber=%d&$sort=%s&$direction=%s", query, pageNumber, sort, direction),
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

    Page<Value> page = new Page<>(List.of(value), DEFAULT_PAGE_NUMBER, pageSize, 1, 1);

    when(dataRepository.findWhere(query, DEFAULT_PAGE_NUMBER, pageSize, emptySet(), ASCENDING)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageSize=%d", query, pageSize),
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageSizeAndSort()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" + value.name();
    int pageSize = 10;

    String sort = "name";
    Direction direction = DESCENDING;

    Page<Value> page = new Page<>(List.of(value), DEFAULT_PAGE_NUMBER, pageSize, 1, 1);

    when(dataRepository.findWhere(query, DEFAULT_PAGE_NUMBER, pageSize, Set.of(sort), direction)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageSize=%d&$sort=%s&$direction=%s", query, pageSize, sort, direction),
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

    Page<Value> page = new Page<>(List.of(value), pageNumber, pageSize, pageSize + 1, 2);

    when(dataRepository.findWhere(query, pageNumber, pageSize, emptySet(), ASCENDING)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageNumber=%d&$pageSize=%d", query, pageNumber, pageSize),
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldGetWithQueryAndPageNumberAndPageSizeAndSort()
  {
    Value value = new Value(123, "TEST");
    String query = "name=" +  value.name();
    int pageNumber = 1;
    int pageSize = 10;

    String sort = "name";
    Direction direction = DESCENDING;

    Page<Value> page = new Page<>(List.of(value), pageNumber, pageSize, pageSize + 1, 2);

    when(dataRepository.findWhere(query, pageNumber, pageSize, Set.of(sort), direction)).thenReturn(page);

    ResponseEntity<Page<Value>> response = testRestTemplate.exchange(
      format("/?q=%s&$pageNumber=%d&$pageSize=%d&$sort=%s&$direction=%s", query, pageNumber, pageSize, sort, direction),
      GET,
      null,
      TYPE_REFERENCE_PAGE
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(page, response.getBody());
  }

  @Test
  void shouldNotGetForUnknownId()
  {
    Integer id = 123;

    when(identityParser.parse(id.toString())).thenReturn(id);
    when(dataRepository.find(id)).thenReturn(Optional.empty());

    ResponseEntity<Value> response = testRestTemplate.getForEntity("/" + id, Value.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldNotGetWithIdAndQuery()
  {
    ResponseEntity<ObjectNode> response = testRestTemplate.getForEntity("/?id=1&q=x", ObjectNode.class);

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
    assertNotNull(response.getBody());
    assertEquals("Illegal query part: x", response.getBody().get(FIELD_MESSAGE).asText());
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

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
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

    assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
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
  void shouldDeleteWithId()
  {
    int id = 123;

    when(identityParser.parse(Integer.toString(id))).thenReturn(id);

    ResponseEntity<Void> response = testRestTemplate.exchange("/" + id, DELETE, null, Void.class);

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(dataRepository).delete(id);
  }

  @Test
  void shouldDeleteWithIds()
  {
    int id1 = 123;
    int id2 = 234;

    when(identityParser.parse(Integer.toString(id1))).thenReturn(id1);
    when(identityParser.parse(Integer.toString(id2))).thenReturn(id2);

    ResponseEntity<Void> response = testRestTemplate.exchange(
      format("/?id=%s&id=%s", id1, id2),
      DELETE,
      null,
      Void.class
    );

    assertTrue(response.getStatusCode().is2xxSuccessful());

    verify(dataRepository).deleteAll(List.of(id1, id2));
  }

  @Test
  void shouldNotDeleteWithRepositoryError()
  {
    int id = 123;

    when(identityParser.parse(Integer.toString(id))).thenReturn(id);
    doThrow(new DeleteException("TESTING")).when(dataRepository).delete(id);

    ResponseEntity<ObjectNode> response = testRestTemplate.exchange("/" + id, DELETE, null, ObjectNode.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("TESTING", response.getBody().get(FIELD_MESSAGE).asText());
  }

  private HttpEntity<?> body(Object body)
  {
    return new HttpEntity<>(body, new LinkedMultiValueMap<>());
  }

  public record Value(Integer id, String name) {}

  static class TestConfiguration
  {
    @Bean
    IdentityParser<Integer> idParser()
    {
      //noinspection unchecked
      return mock(IdentityParser.class);
    }

    @Bean
    JsonCodec<Value> jsonCodec(ObjectMapper objectMapper)
    {
      return new DefaultJsonCodec<>(objectMapper, Value.class);
    }

    @Bean
    QueryBuilder queryBuilder()
    {
      return new QueryBuilder(Value.class);
    }

    @Bean
    DataRepository<Integer, Value> dataRepository()
    {
      //noinspection unchecked
      return mock(DataRepository.class);
    }
  }
}

