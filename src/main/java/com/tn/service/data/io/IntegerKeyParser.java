package com.tn.service.data.io;

public class IntegerKeyParser implements KeyParser<Integer>
{
  @Override
  public Integer parse(String key) throws InvalidKeyException
  {
    try
    {
      return Integer.parseInt(key);
    }
    catch (NumberFormatException e)
    {
      throw new InvalidKeyException("Invalid key: " + key, e);
    }
  }
}
