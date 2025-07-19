package com.tn.service.data.domain;

public enum Direction
{
  ASCENDING,
  DESCENDING;

  public boolean isDescending()
  {
    return this.equals(DESCENDING);
  }
}
