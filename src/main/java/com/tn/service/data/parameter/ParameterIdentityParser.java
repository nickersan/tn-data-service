package com.tn.service.data.parameter;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;

import org.springframework.util.MultiValueMap;

import com.tn.service.IllegalParameterException;

public class ParameterIdentityParser<T> implements IdentityParser<MultiValueMap<String, String>, Collection<T>>
{
  private final IdentityParser<String, T> identityParser;
  private final String paramName;

  public ParameterIdentityParser(IdentityParser<String, T> identityParser, String paramName)
  {
    this.identityParser = identityParser;
    this.paramName = paramName;
  }

  @Override
  public Collection<T> parse(MultiValueMap<String, String> params) throws IllegalParameterException
  {
    checkParams(params);
    List<String> identityParams = params.get(paramName);

    return identityParams == null ? emptySet() : identityParams.stream().map(identityParser::parse).collect(toSet());
  }

  private void checkParams(MultiValueMap<String, String> params)
  {
    if (params.containsKey(paramName) && params.keySet().stream().filter(Parameters::isNotSort).count() > 1)
    {
      throw new IllegalParameterException("Identity parameters can only be used in isolation from other non-sort parameters");
    }
  }
}
