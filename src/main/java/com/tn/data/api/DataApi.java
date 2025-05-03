package com.tn.data.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface DataApi
{
  @GetMapping(value = "/{key}", produces = APPLICATION_JSON_VALUE)
  ResponseEntity<? extends JsonNode> get(@PathVariable("key") String key);

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  ResponseEntity<? extends JsonNode> get(
    @RequestParam(value = "key") Collection<String> keys,
    @RequestParam(value = "q", required = false) String query,
    @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  );

  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  ResponseEntity<? extends JsonNode> post(RequestEntity<JsonNode> request);

  @PutMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  ResponseEntity<? extends JsonNode> put(RequestEntity<JsonNode> request);

  @DeleteMapping(value = "/{key}")
  ResponseEntity<Void> delete(@PathVariable("key") String key);

  @DeleteMapping
  ResponseEntity<Void> delete(@RequestParam(value = "key") Collection<String> keys);
}
