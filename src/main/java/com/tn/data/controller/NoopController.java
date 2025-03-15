package com.tn.data.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.tn.data.api.NoopApi;

@Slf4j
@RestController
public class NoopController implements NoopApi
{
  public ResponseEntity<Void> noop()
  {
    log.debug("Received GET /noop");
    return ResponseEntity.ok().build();
  }
}
