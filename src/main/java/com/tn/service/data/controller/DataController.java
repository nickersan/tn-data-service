package com.tn.service.data.controller;

import static java.util.Collections.emptySet;

import static com.tn.lang.Objects.coalesce;
import static com.tn.lang.Strings.isNotNullOrWhitespace;
import static com.tn.service.data.domain.Direction.ASCENDING;

import java.util.Collection;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tn.query.QueryParseException;
import com.tn.service.IllegalParameterException;
import com.tn.service.data.api.DataApi;
import com.tn.service.data.domain.Direction;
import com.tn.service.data.io.JsonCodec;
import com.tn.service.data.io.JsonException;
import com.tn.service.data.parameter.IdentityParser;
import com.tn.service.data.parameter.ParameterIdentityParser;
import com.tn.service.data.parameter.QueryBuilder;
import com.tn.service.data.repository.DataRepository;
import com.tn.service.data.repository.DeleteException;
import com.tn.service.data.repository.InsertException;
import com.tn.service.data.repository.RepositoryException;
import com.tn.service.data.repository.UpdateException;

@Slf4j
@RestController
@RequestMapping("${tn.service.data.path.root:}")
@ConditionalOnWebApplication
@ConditionalOnBean({DataRepository.class, IdentityParser.class, JsonCodec.class, QueryBuilder.class})
public class DataController<ID, V> implements DataApi
{
  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final String FIELD_MESSAGE = "message";
  private static final String FIELD_DETAIL = "detail";

  private final DataRepository<ID, V> dataRepository;
  private final IdentityParser<String, ID> identityParser;
  private final Validator validator;
  private final IdentityParser<MultiValueMap<String, String>, Collection<ID>> parameterIdentityParser;
  private final JsonCodec<V>  jsonCodec;
  private final QueryBuilder queryBuilder;

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public DataController(
    IdentityParser<String, ID> identityParser,
    JsonCodec<V> jsonCodec,
    Validator validator,
    QueryBuilder queryBuilder,
    DataRepository<ID, V> dataRepository,
    @Value("${tn.service.data.identity.param-name:id}") String identityParameterName
  )
  {
    this.identityParser = identityParser;
    this.validator = validator;
    this.parameterIdentityParser = new ParameterIdentityParser<>(identityParser, identityParameterName);
    this.jsonCodec = jsonCodec;
    this.queryBuilder = queryBuilder;
    this.dataRepository = dataRepository;
  }

  @Override
  public ResponseEntity<? extends JsonNode> get(String id)
  {
    return dataRepository.find(identityParser.parse(id))
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
    Collection<ID> identities = parameterIdentityParser.parse(params);
    if (!identities.isEmpty())
    {
      return ResponseEntity.ok(jsonCodec.writeValue(dataRepository.findAll(identities)));
    }
    else
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
  }

  @Override
  public ResponseEntity<? extends JsonNode> post(RequestEntity<JsonNode> request)
  {
    if (request.getBody() instanceof ObjectNode)
    {
      V entity = jsonCodec.readValue(request.getBody());
      Collection<ConstraintViolation<V>> constraintViolations = validate(entity);
      if (!constraintViolations.isEmpty()) return ResponseEntity.badRequest().body(error(constraintViolations));

      return ResponseEntity.ok(jsonCodec.writeValue(dataRepository.insert(entity)));
    }
    else if (request.getBody() instanceof ArrayNode)
    {
      Collection<V> entities = jsonCodec.readValues((ArrayNode)request.getBody());
      Collection<ConstraintViolation<V>> constraintViolations = validateAll(entities);
      if (!constraintViolations.isEmpty()) return ResponseEntity.badRequest().body(error(constraintViolations));

      return ResponseEntity.ok(jsonCodec.writeValues(dataRepository.insertAll(entities)));
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
      V entity = jsonCodec.readValue(request.getBody());
      Collection<ConstraintViolation<V>> constraintViolations = validate(entity);
      if (!constraintViolations.isEmpty()) return ResponseEntity.badRequest().body(error(constraintViolations));

      return ResponseEntity.ok(jsonCodec.writeValue(dataRepository.update(entity)));
    }
    else if (request.getBody() instanceof ArrayNode)
    {
      Collection<V> entities = jsonCodec.readValues((ArrayNode)request.getBody());
      Collection<ConstraintViolation<V>> constraintViolations = validateAll(entities);
      if (!constraintViolations.isEmpty()) return ResponseEntity.badRequest().body(error(constraintViolations));

      return ResponseEntity.ok(jsonCodec.writeValues(dataRepository.updateAll(entities)));
    }
    else
    {
      return invalidBody();
    }
  }

  @Override
  public ResponseEntity<? extends JsonNode> delete(String key)
  {
    return dataRepository.delete(identityParser.parse(key))
      .map(value -> ResponseEntity.ok(jsonCodec.writeValue(value)))
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<? extends JsonNode> delete(MultiValueMap<String, String> params)
  {
    Collection<ID> identities = parameterIdentityParser.parse(params);
    return !identities.isEmpty()
      ? ResponseEntity.ok(jsonCodec.writeValue(dataRepository.deleteAll(identities)))
      : ResponseEntity.badRequest().body(error("Cannot delete all, identity parameters must be specified"));
  }

  private Collection<ConstraintViolation<V>> validate(V entity)
  {
    return validator.validate(entity);
  }

  private Collection<ConstraintViolation<V>> validateAll(Collection<V> entities)
  {
    return entities.stream()
      .map(this::validate)
      .flatMap(Collection::stream)
      .toList();
  }

  @ExceptionHandler({InsertException.class, UpdateException.class, DeleteException.class})
  ResponseEntity<ObjectNode> internalServerError(RepositoryException e)
  {
    log.error("Data controller error", e);
    return ResponseEntity.internalServerError().body(error(e.getMessage()));
  }

  @ExceptionHandler({ClassCastException.class, JsonException.class})
  ResponseEntity<ObjectNode> invalidBody(RuntimeException e)
  {
    log.error("Data controller error", e);
    return invalidBody();
  }

  @ExceptionHandler({IllegalParameterException.class, QueryParseException.class})
  ResponseEntity<ObjectNode> badRequest(RuntimeException e)
  {
    log.error("Data controller error", e);
    return ResponseEntity.badRequest().body(error(e.getMessage()));
  }

  private ResponseEntity<ObjectNode> invalidBody()
  {
    return ResponseEntity.badRequest().body(error("Invalid body"));
  }

  private ObjectNode error(Collection<ConstraintViolation<V>> constraintViolations)
  {
    ObjectNode error = error("Invalid body");
    error.set(
      FIELD_DETAIL,
      new ArrayNode(
        null,
        constraintViolations.stream()
          .map(constraintViolation -> (JsonNode)TextNode.valueOf(constraintViolation.getPropertyPath() + " " + constraintViolation.getMessage()))
          .toList()
      )
    );

    return error;
  }

  private ObjectNode error(String message)
  {
    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf(message));

    return error;
  }
}
