package com.tn.data.controller;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.tn.data.api.DataApi;
import com.tn.data.io.KeyParser;
import com.tn.data.repository.DataRepository;

@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnWebApplication
@ConditionalOnBean({DataRepository.class, KeyParser.class})
public class DataController implements DataApi
{
  private final DataRepository dataRepository;
  private final KeyParser keyParser;

  @Override
  public ResponseEntity<ObjectNode> find(String key)
  {
    return dataRepository.find(keyParser.parse(key))
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Collection<ObjectNode>> findAll(Collection<String> keys)
  {
    return ResponseEntity.ok(dataRepository.findAll(keys.stream().map(keyParser::parse).toArray(ObjectNode[]::new)));
  }
}
