package com.tn.service.data.io;

public class IntegerIdParser implements IdParser<Integer>
{
  @Override
  public Integer parse(String key) throws InvalidIdException
  {
    try
    {
      return Integer.parseInt(key);
    }
    catch (NumberFormatException e)
    {
      throw new InvalidIdException("Invalid key: " + key, e);
    }
  }
}
