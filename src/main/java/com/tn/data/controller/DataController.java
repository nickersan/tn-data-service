package com.tn.data.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.tn.data.api.DataApi;

@Slf4j
@RequiredArgsConstructor
@RestController
public class DataController implements DataApi
{
//  private final DataRepository dataRepository;

  @Override
  public ResponseEntity<ObjectNode> find(String key)
  {
    return null; //dataRepository.find();
  }
}
