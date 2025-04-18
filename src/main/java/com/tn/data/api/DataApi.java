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
  ResponseEntity<ObjectNode> find(@PathVariable("key") String key);

  @GetMapping
  ResponseEntity<Collection<ObjectNode>> findAll(@RequestParam("key") Collection<String> keys);
}
