package com.tn.service.data.controller;

import static java.util.Collections.emptySet;

import static com.tn.lang.Objects.coalesce;
import static com.tn.lang.Strings.isNotNullOrWhitespace;
import static com.tn.service.data.domain.Direction.ASCENDING;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tn.query.QueryParseException;
import com.tn.service.data.api.DataApi;
import com.tn.service.data.domain.Direction;
import com.tn.service.data.io.IdParser;
import com.tn.service.data.io.JsonCodec;
import com.tn.service.data.io.JsonException;
import com.tn.service.data.query.QueryBuilder;
import com.tn.service.data.repository.DataRepository;
import com.tn.service.data.repository.DeleteException;
import com.tn.service.data.repository.InsertException;
import com.tn.service.data.repository.RepositoryException;
import com.tn.service.data.repository.UpdateException;

@Slf4j
@RestController
@RequestMapping("${tn.service.data.path.root:}")
@ConditionalOnWebApplication
@ConditionalOnBean({DataRepository.class, IdParser.class, JsonCodec.class, QueryBuilder.class})
public class DataController<K, V> implements DataApi
{
  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final String FIELD_MESSAGE = "message";

  private final DataRepository<K, V> dataRepository;
  private final IdParser<K> idParser;
  private final JsonCodec<V>  jsonCodec;
  private final QueryBuilder queryBuilder;

  public DataController(IdParser<K> idParser, JsonCodec<V> jsonCodec, QueryBuilder queryBuilder, DataRepository<K, V> dataRepository)
  {
    this.idParser = idParser;
    this.jsonCodec = jsonCodec;
    this.queryBuilder = queryBuilder;
    this.dataRepository = dataRepository;
  }

  @Override
  public ResponseEntity<? extends JsonNode> get(String id)
  {
    return dataRepository.find(parseKey(id))
      .map(value -> ResponseEntity.ok(jsonCodec.writeValue(value)))
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<? extends JsonNode> get(
    MultiValueMap<String, String> params,
    Integer pageNumber,
    Integer pageSize,
    Collection<String> sort,
    Direction direction
  )
  {
    String query = queryBuilder.build(params);

    if (isNotNullOrWhitespace(query))
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null
          ? jsonCodec.writeValue(dataRepository.findWhere(query, coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE), coalesce(sort, emptySet()), coalesce(direction, ASCENDING)))
          : jsonCodec.writeValues(dataRepository.findWhere(query, coalesce(sort, emptySet()), coalesce(direction, ASCENDING)))
      );
    }
    else
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null
          ? jsonCodec.writeValue(dataRepository.findAll(coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE), coalesce(sort, emptySet()), direction))
          : jsonCodec.writeValues(dataRepository.findAll(coalesce(sort, emptySet()), coalesce(direction, ASCENDING)))
      );
    }
  }

  @Override
  public ResponseEntity<? extends JsonNode> post(RequestEntity<JsonNode> request)
  {
    if (request.getBody() instanceof ObjectNode)
    {
      return ResponseEntity.ok(jsonCodec.writeValue(dataRepository.insert(jsonCodec.readValue(request.getBody()))));
    }
    else if (request.getBody() instanceof ArrayNode)
    {
      return ResponseEntity.ok(jsonCodec.writeValues(dataRepository.insertAll(jsonCodec.readValues((ArrayNode)request.getBody()))));
    }
    else
    {
      return invalidBody();
    }
  }

  @Override
  public ResponseEntity<? extends JsonNode> put(RequestEntity<JsonNode> request)
  {
    if (request.getBody() instanceof ObjectNode)
    {
      return ResponseEntity.ok(jsonCodec.writeValue(dataRepository.update(jsonCodec.readValue(request.getBody()))));
    }
    else if (request.getBody() instanceof ArrayNode)
    {
      return ResponseEntity.ok(jsonCodec.writeValues(dataRepository.updateAll(jsonCodec.readValues((ArrayNode)request.getBody()))));
    }
    else
    {
      return invalidBody();
    }
  }

  @Override
  public ResponseEntity<Void> delete(String key)
  {
    dataRepository.delete(idParser.parse(key));
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
    return ResponseEntity.internalServerError().body(error(e.getMessage()));
  }

  @ExceptionHandler({ ClassCastException.class, JsonException.class })
  ResponseEntity<ObjectNode> invalidBody(RuntimeException e)
  {
    log.error("Handling error", e);
    return invalidBody();
  }

  @ExceptionHandler(QueryParseException.class)
  ResponseEntity<ObjectNode> queryParse(QueryParseException e)
  {
    log.error("Handling error", e);
    return ResponseEntity.badRequest().body(error(e.getMessage()));
  }

  private ResponseEntity<ObjectNode> invalidBody()
  {
    return ResponseEntity.badRequest().body(error("Invalid body"));
  }

  private ObjectNode error(String message)
  {
    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf(message));

    return error;
  }

  private Iterable<K> parseKeys(Collection<String> keys)
  {
    return keys.stream().map(this::parseKey).toList();
  }

  private K parseKey(String key)
  {
    return idParser.parse(key);
  }
}
