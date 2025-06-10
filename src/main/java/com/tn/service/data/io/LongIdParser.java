package com.tn.service.data.io;

public class LongIdParser implements IdParser<Long>
{
  @Override
  public Long parse(String key) throws InvalidIdException
  {
    try
    {
      return Long.parseLong(key);
    }
    catch (NumberFormatException e)
    {
      throw new InvalidIdException("Invalid key: " + key, e);
    }
  }
}
