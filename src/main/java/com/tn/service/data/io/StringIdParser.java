package com.tn.service.data.io;

public class StringIdParser implements IdParser<String>
{
  @Override
  public String parse(String key) throws InvalidIdException
  {
    return key;
  }
}
