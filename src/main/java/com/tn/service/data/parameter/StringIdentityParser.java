package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public class StringIdentityParser extends AbstractIdentityParser<String>
{
  public StringIdentityParser(String paramName)
  {
    super(paramName);
  }

  @Override
  protected String parseIdentity(String s) throws IllegalParameterException
  {
    return s;
  }
}
