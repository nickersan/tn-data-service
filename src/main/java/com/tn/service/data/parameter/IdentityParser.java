package com.tn.service.data.parameter;

import com.tn.service.IllegalParameterException;

public interface IdentityParser<T, R>
{
  R parse(T raw) throws IllegalParameterException;
}
