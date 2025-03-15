package com.tn.data.repository;

import com.tn.data.domain.Field;
import com.tn.query.QueryParser;
import com.tn.query.jdbc.JdbcPredicate;

import javax.sql.DataSource;
import java.util.Collection;

public class JdbcDataRepository implements DataRepository
{
  private final DataSource dataSource;
  private final Collection<Field> fields;
  private final QueryParser<JdbcPredicate> queryParser;
  private final String selectQuery;

  public JdbcDataRepository(
    DataSource dataSource,
    String schema,
    String table,
    Collection<Field> fields,
    QueryParser<JdbcPredicate> queryParser
  )
  {
    this.dataSource = dataSource;
    this.fields = fields;
    this.queryParser = queryParser;

    this.selectQuery = selectQuery(schema, table, fields);
  }

  private String selectQuery(String schema, String table, Collection<Field> fields)
  {
    return null;
  }
}
