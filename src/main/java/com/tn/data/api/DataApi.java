package com.tn.data.api;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface DataApi
{
  @GetMapping("/{key}")
  ResponseEntity<ObjectNode> get(@PathVariable("key") String key);

  @GetMapping
  ResponseEntity<Iterable<ObjectNode>> get(
    @RequestParam(value = "key") Collection<String> keys,
    @RequestParam(value = "q", required = false) String query,
    @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
    @RequestParam(value = "pageSize", required = false) Integer pageSize
  );
}
