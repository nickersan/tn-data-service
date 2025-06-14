package com.tn.service.data.parameter;

import java.util.Collection;

import org.springframework.util.MultiValueMap;

import com.tn.service.IllegalParameterException;

public interface IdentityParser<T>
{
  Collection<T> parse(MultiValueMap<String, String> params) throws IllegalParameterException;
}
