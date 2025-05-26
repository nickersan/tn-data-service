package com.tn.service.data.io;

public class StringKeyParser implements KeyParser<String>
{
  @Override
  public String parse(String key) throws InvalidKeyException
  {
    return key;
  }
}
