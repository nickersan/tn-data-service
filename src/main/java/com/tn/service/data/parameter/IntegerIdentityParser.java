package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public class IntegerIdentityParser implements IdentityParser<String, Integer>
{
  @Override
  public Integer parse(String s) throws IllegalParameterException
  {
    try
    {
      return Integer.parseInt(s);
    }
    catch (NumberFormatException e)
    {
      throw new IllegalParameterException("Invalid identity parameter: " + s, e);
    }
  }
}
