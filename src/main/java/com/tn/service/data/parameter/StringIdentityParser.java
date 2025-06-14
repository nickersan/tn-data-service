package com.tn.service.data.parameter;

public class StringIdentityParser implements IdentityParser<String, String>
{
  @Override
  public String parse(String s)
  {
    return s;
  }
}
