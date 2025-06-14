package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public class LongIdentityParser extends AbstractIdentityParser<Long>
{
  public LongIdentityParser(String paramName)
  {
    super(paramName);
  }

  @Override
  protected Long parseIdentity(String s) throws IllegalParameterException
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
