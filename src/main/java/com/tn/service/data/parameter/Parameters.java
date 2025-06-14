package com.tn.service.data.parameter;

import java.util.Collection;
import java.util.Set;

public class Parameters
{
  public static final Collection<String> RESERVED_PARAMS = Set.of("$pageNumber", "$pageSize", "$sort", "$direction");
  public static final Collection<String> SORT_PARAMS = Set.of("$sort", "$direction");

  private Parameters() {}

  public static boolean isNotReserved(String paramName)
  {
    return !RESERVED_PARAMS.contains(paramName);
  }

  public static boolean isNotSort(String paramName)
  {
    return !SORT_PARAMS.contains(paramName);
  }
}
