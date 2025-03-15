package com.tn.data.repository;

import com.tn.data.domain.Field;
import com.tn.data.util.Fields;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;

public class JdbcFieldRepository implements FieldRepository
{
  private final DataSource dataSource;

  public JdbcFieldRepository(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  @Override
  public Collection<Field> findForTable(String schema, String table) throws FindException
  {
    try
    {
      return Fields.findAll(dataSource, schema, table);
    }
    catch (SQLException e)
    {
      throw new FindException(e);
    }
  }
}
