package com.tn.data.repository;

import com.tn.data.domain.Field;

import java.util.Collection;

public interface FieldRepository
{
  Collection<Field> findForTable(String schema, String table) throws FindException;
}
