package com.tn.service.data.io;

public class LongKeyParser implements KeyParser<Long>
{
  @Override
  public Long parse(String key) throws InvalidKeyException
  {
    try
    {
      return Long.parseLong(key);
    }
    catch (NumberFormatException e)
    {
      throw new InvalidKeyException("Invalid key: " + key, e);
    }
  }
}
