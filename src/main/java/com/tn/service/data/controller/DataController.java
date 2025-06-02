package com.tn.service.data.controller;

import static java.util.Collections.emptySet;

import static com.tn.lang.Iterables.isNotEmpty;
import static com.tn.lang.Objects.coalesce;
import static com.tn.lang.Strings.isNotNullOrWhitespace;
import static com.tn.lang.util.function.Lambdas.wrapFunction;

import java.util.Collection;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tn.lang.util.Page;
import com.tn.lang.util.function.WrappedException;
import com.tn.service.data.api.DataApi;
import com.tn.service.data.domain.Direction;
import com.tn.service.data.io.KeyParser;
import com.tn.service.data.repository.DataRepository;
import com.tn.service.data.repository.DeleteException;
import com.tn.service.data.repository.InsertException;
import com.tn.service.data.repository.RepositoryException;
import com.tn.service.data.repository.UpdateException;

@Slf4j
@RestController
@ConditionalOnWebApplication
@ConditionalOnBean({DataRepository.class, KeyParser.class})
@ConditionalOnProperty("tn.data.value-class")
public class DataController<K, V> implements DataApi
{
  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final String FIELD_MESSAGE = "message";

  private final DataRepository<K, V> dataRepository;
  private final KeyParser<K> keyParser;
  private final ObjectMapper objectMapper;
  private final Class<V> valueClass;

  public DataController(
    DataRepository<K, V> dataRepository,
    KeyParser<K> keyParser,
    ObjectMapper objectMapper,
    @Value("${tn.data.value-class}") Class<V> valueClass
  )
  {
    this.dataRepository = dataRepository;
    this.keyParser = keyParser;
    this.objectMapper = objectMapper;
    this.valueClass = valueClass;
  }

  @Override
  public ResponseEntity<? extends JsonNode> get(String key)
  {
    return dataRepository.find(parseKey(key))
      .map(value -> ResponseEntity.ok(objectNode(value)))
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<? extends JsonNode> get(
    @RequestParam(value = "key", required = false) Collection<String> keys,
    @RequestParam(value = "q", required = false) String query,
    @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    @RequestParam(value = "sort", required = false) Collection<String> sort,
    @RequestParam(value = "direction", defaultValue = "ASCENDING") Direction direction
  )
  {
    if (isNotEmpty(keys))
    {
      if (query != null)
      {
        return badRequest("Query parameter not allowed with key(s)");
      }
      if (pageNumber != null || pageSize != null)
      {
        return badRequest("Pagination not supported with Key(s)");
      }

      return ResponseEntity.ok(arrayNode(dataRepository.findAll(parseKeys(keys), coalesce(sort, emptySet()), direction)));
    }
    else if (isNotNullOrWhitespace(query))
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null
          ? objectNode(dataRepository.findWhere(query, coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE), coalesce(sort, emptySet()), direction))
          : arrayNode(dataRepository.findWhere(query, coalesce(sort, emptySet()), direction))
      );
    }
    else
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null
          ? objectNode(dataRepository.findAll(coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE), coalesce(sort, emptySet()), direction))
          : arrayNode(dataRepository.findAll(coalesce(sort, emptySet()), direction))
      );
    }
  }

  @Override
  public ResponseEntity<? extends JsonNode> post(RequestEntity<JsonNode> request)
  {
    try
    {
      if (request.getBody() instanceof ObjectNode)
      {
        return ResponseEntity.ok(objectNode(dataRepository.insert(value(request.getBody()))));
      }
      else if (request.getBody() instanceof ArrayNode)
      {
        return ResponseEntity.ok(arrayNode(dataRepository.insertAll(values(request.getBody()))));
      }
      else
      {
        return badRequest("Invalid body");
      }
    }
    catch (JsonProcessingException | ClassCastException e)
    {
      return badRequest("Invalid body");
    }
  }

  @Override
  public ResponseEntity<? extends JsonNode> put(RequestEntity<JsonNode> request)
  {
    try
    {
      if (request.getBody() instanceof ObjectNode)
      {
        return ResponseEntity.ok(objectNode(dataRepository.update(value(request.getBody()))));
      }
      else if (request.getBody() instanceof ArrayNode)
      {
        return ResponseEntity.ok(arrayNode(dataRepository.updateAll(values(request.getBody()))));
      }
      else
      {
        return badRequest("Invalid body");
      }
    }
    catch (JsonProcessingException | ClassCastException e)
    {
      return badRequest("Invalid body");
    }
  }

  @Override
  public ResponseEntity<Void> delete(String key)
  {
    dataRepository.delete(keyParser.parse(key));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> delete(Collection<String> keys)
  {
    dataRepository.deleteAll(parseKeys(keys));
    return ResponseEntity.ok().build();
  }

  @ExceptionHandler({InsertException.class, UpdateException.class, DeleteException.class})
  ResponseEntity<ObjectNode> internalServerError(RepositoryException e)
  {
    log.error("Handling error", e);

    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf(e.getMessage()));

    return ResponseEntity.internalServerError().body(error);
  }

  @ExceptionHandler(ClassCastException.class)
  ResponseEntity<ObjectNode> classCast(ClassCastException e)
  {
    log.error("Handling error", e);

    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf("Invalid body"));

    return ResponseEntity.badRequest().body(error);
  }

  private ContainerNode<?> arrayNode(Collection<V> values)
  {
    return objectMapper.createArrayNode().addAll(values.stream().map(this::objectNode).toList());
  }

  private ObjectNode objectNode(V value)
  {
    return value instanceof ObjectNode ? (ObjectNode)value : objectMapper.valueToTree(value);
  }

  private V value(JsonNode node) throws JsonProcessingException
  {
    return ObjectNode.class.equals(valueClass) ? valueClass.cast(node) : objectMapper.treeToValue(node, valueClass);
  }

  private Iterable<V> values(JsonNode array) throws JsonProcessingException
  {
    try
    {
      return StreamSupport.stream(array.spliterator(), false).map(wrapFunction(this::value)).toList();
    }
    catch (WrappedException e)
    {
      if (e.getCause() instanceof JsonProcessingException)
      {
        throw (JsonProcessingException)e.getCause();
      }
      else
      {
        throw (RuntimeException)e.getCause();
      }
    }
  }

  private ContainerNode<?> objectNode(Page<V> page)
  {
    return objectMapper.valueToTree(page);
  }

  private Iterable<K> parseKeys(Collection<String> keys)
  {
    return keys.stream().map(this::parseKey).toList();
  }

  private K parseKey(String key)
  {
    return keyParser.parse(key);
  }

  ResponseEntity<ObjectNode> badRequest(String message)
  {
    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf(message));

    return ResponseEntity.badRequest().body(error);
  }
}
