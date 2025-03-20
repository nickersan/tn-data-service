package com.tn.data.repository;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tn.data.domain.Field;
import com.tn.data.util.Fields;
import com.tn.query.QueryParser;
import com.tn.query.jdbc.JdbcPredicate;

public class JdbcDataRepository implements DataRepository
{
  private static final String COLUMN_SEPARATOR = ", ";
  private static final String SELECT = "SELECT %s FROM %s.%s";
  private static final String WHERE = "%s WHERE %s";

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
    return format(
      SELECT,
      fields.stream().map(field -> field.column().name()).collect(joining(COLUMN_SEPARATOR)),
      schema,
      table
    );
  }

  @Override
  public Collection<ObjectNode> findAll() throws FindException
  {
    return new JdbcTemplate(dataSource).query(selectQuery, this::toObject);
  }

  @Override
  public Collection<ObjectNode> findFor(String query) throws FindException
  {
    JdbcPredicate predicate = queryParser.parse(query);

    //noinspection SqlSourceToSinkFlow
    return new JdbcTemplate(dataSource).query(
      WHERE.formatted(selectQuery, predicate.toSql()),
      predicate::setValues,
      this::toObject
    );
  }

  private ObjectNode toObject(ResultSet resultSet, int i)
  {
    ObjectNode object = new ObjectNode(null);
    fields.forEach(field -> setField(object, field.name(), read(field, resultSet)));

    return object;
  }

  private void setField(ObjectNode object, String name, JsonNode value)
  {
    if (value != null) object.set(name, value);
  }

  private JsonNode read(Field field, ResultSet resultSet)
  {
    try
    {
      return Fields.read(field, resultSet);
    }
    catch (SQLException e)
    {
      throw new FindException(e);
    }
  }
}
