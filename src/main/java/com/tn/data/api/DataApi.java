package com.tn.data.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface DataApi
{
  @GetMapping("/{key}")
  ResponseEntity<ObjectNode> find(@RequestParam("key") String key);
}
