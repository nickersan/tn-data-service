package com.tn.service.data.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Versioned<T extends Versioned<?>>
{
//  long version();

  @JsonIgnore
  T nextVersion();
}
