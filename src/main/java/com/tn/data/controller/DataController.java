package com.tn.data.controller;

import static com.tn.lang.Iterables.isNotEmpty;
import static com.tn.lang.Objects.coalesce;
import static com.tn.lang.Strings.isNotNullOrWhitespace;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tn.data.api.DataApi;
import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;
import com.tn.lang.util.Page;

@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnWebApplication
@ConditionalOnBean({DataRepository.class, KeyParser.class})
public class DataController implements DataApi
{
  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 100;

  private static final String FIELD_MESSAGE = "message";

  private final DataRepository dataRepository;
  private final KeyParser keyParser;
  private final ObjectMapper objectMapper;

  @Override
  public ResponseEntity<ObjectNode> get(String key)
  {
    return dataRepository.find(keyParser.parse(key))
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<ContainerNode<?>> get(
    @RequestParam(value = "key", required = false) Collection<String> keys,
    @RequestParam(value = "q", required = false) String query,
    @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  )
  {
    if (isNotEmpty(keys))
    {
      if (query != null) throw new BadRequestException("Query parameter not allowed with key(s)");
      if (pageNumber != null || pageSize != null) throw new BadRequestException("Pagination not supported with Key(s)");

      return ResponseEntity.ok(arrayNode(dataRepository.findAll(parseKeys(keys))));
    }
    else if (isNotNullOrWhitespace(query))
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null ?
          objectNode(dataRepository.findFor(query, coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE))) :
          arrayNode(dataRepository.findFor(query))
      );
    }
    else
    {
      return ResponseEntity.ok(
        pageNumber != null || pageSize != null ?
          objectNode(dataRepository.findAll(coalesce(pageNumber, DEFAULT_PAGE_NUMBER), coalesce(pageSize, DEFAULT_PAGE_SIZE))) :
          arrayNode(dataRepository.findAll())
      );
    }
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ObjectNode> badRequest(BadRequestException e)
  {
    ObjectNode error = new ObjectNode(null);
    error.set(FIELD_MESSAGE, TextNode.valueOf(e.getMessage()));

    return  ResponseEntity.badRequest().body(error);
  }

  private ContainerNode<?> arrayNode(Collection<ObjectNode> objects)
  {
    return objectMapper.createArrayNode().addAll(objects);
  }

  private ContainerNode<?> objectNode(Page<ObjectNode> page)
  {
    return objectMapper.valueToTree(page);
  }

  private Iterable<ObjectNode> parseKeys(Collection<String> keys)
  {
    return keys.stream().map(keyParser::parse).toList();
  }
}
