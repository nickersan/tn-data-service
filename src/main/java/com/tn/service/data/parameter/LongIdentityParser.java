package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public class LongIdentityParser implements IdentityParser<String, Long>
{
  @Override
  public Long parse(String s) throws IllegalParameterException
  {
    try
    {
      return Long.parseLong(s);
    }
    catch (NumberFormatException e)
    {
      throw new IllegalParameterException("Invalid identity parameter: " + s, e);
    }
  }
}
