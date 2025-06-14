package com.tn.service.data.parameter;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;

import org.springframework.util.MultiValueMap;

import com.tn.service.IllegalParameterException;

public abstract class AbstractIdentityParser<T> implements IdentityParser<T>
{
  private final String identityParamName;

  public AbstractIdentityParser(String identityParamName)
  {
    this.identityParamName = identityParamName;
  }

  @Override
  public Collection<T> parse(MultiValueMap<String, String> params) throws IllegalParameterException
  {
    checkParams(params);
    List<String> identityParams = params.get(identityParamName);

    return identityParams == null ? null : identityParams.stream().map(this::parseIdentity).collect(toSet());
  }

  protected abstract T parseIdentity(String s) throws IllegalParameterException;

  private void checkParams(MultiValueMap<String, String> params)
  {
    if (params.containsKey(identityParamName) && params.keySet().stream().filter(Parameters::isNotSort).count() > 1)
    {
      throw new IllegalParameterException("Identity parameters can only be used in isolation from other non-sort parameters");
    }
  }
}
