package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public class IntegerIdentityParser extends AbstractIdentityParser<Integer>
{
  public IntegerIdentityParser(String paramName)
  {
    super(paramName);
  }

  @Override
  protected Integer parseIdentity(String s) throws IllegalParameterException
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
