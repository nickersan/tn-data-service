package com.tn.data.api;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface DataApi
{
  @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ObjectNode> get(@PathVariable("key") String key);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ContainerNode<?>> get(
    @RequestParam(value = "key") Collection<String> keys,
    @RequestParam(value = "q", required = false) String query,
    @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  );

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ContainerNode<?>> post(RequestEntity<ContainerNode<?>> request);
}
