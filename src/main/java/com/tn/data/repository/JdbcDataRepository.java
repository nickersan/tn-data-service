package com.tn.data.repository;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import static com.tn.lang.util.function.Lambdas.wrapConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.tn.data.domain.Field;
import com.tn.data.util.Fields;
import com.tn.lang.sql.PreparedStatements;
import com.tn.lang.util.function.ConsumerWithThrows;
import com.tn.lang.util.function.WrappedException;
import com.tn.query.QueryParser;
import com.tn.query.jdbc.JdbcPredicate;

public class JdbcDataRepository implements DataRepository
{
  private static final String COLUMN_PLACEHOLDER = "?";
  private static final String COLUMN_SEPARATOR = ", ";
  private static final int DEFAULT_BATCH_SIZE = 50;
  private static final String SELECT = "SELECT %s FROM %s.%s";
  private static final String INSERT = "INSERT INTO %s.%s(%s) VALUES (%s)";
  private static final String WHERE = "%s WHERE %s";

  private final DataSource dataSource;
  private final Collection<Field> fields;
  private final QueryParser<JdbcPredicate> queryParser;
  private final String select;
  private final String insert;

  private int batchSize = DEFAULT_BATCH_SIZE;

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

    this.select = select(schema, table, fields);
    this.insert = insert(schema, table, fields);
  }

  public JdbcDataRepository withBatchSize(int batchSize)
  {
    this.batchSize = batchSize;
    return this;
  }

  @Override
  public Collection<ObjectNode> findAll() throws FindException
  {
    try
    {
      return new JdbcTemplate(dataSource).query(select, this::toObject);
    }
    catch (DataAccessException e)
    {
      throw new FindException(e.getCause());
    }
  }

  @Override
  public Collection<ObjectNode> findFor(String query) throws FindException
  {
    try
    {
      JdbcPredicate predicate = queryParser.parse(query);

      //noinspection SqlSourceToSinkFlow
      return new JdbcTemplate(dataSource).query(
        WHERE.formatted(select, predicate.toSql()),
        predicate::setValues,
        this::toObject
      );
    }
    catch (DataAccessException e)
    {
      throw new FindException(e.getCause());
    }
  }

  @Override
  @Transactional
  public ObjectNode insert(ObjectNode object) throws InsertException
  {
    try
    {
      new JdbcTemplate(dataSource).update(insert, preparedStatement -> setValues(preparedStatement, new AtomicInteger(1), object));

      //TODO: workout what to do with primary keys.
      return object;
    }
    catch (DataAccessException e)
    {
      throw new InsertException(e.getCause());
    }
  }

  @Override
  @Transactional
  public Collection<ObjectNode> insert(Collection<ObjectNode> objects) throws InsertException
  {
    try
    {
      new JdbcTemplate(dataSource).batchUpdate(
        insert,
        objects,
        10,
        (preparedStatement, object) -> setValues(preparedStatement, new AtomicInteger(1), object)
      );

      return objects;
    }
    catch (DataAccessException e)
    {
      throw new InsertException(e.getCause());
    }
  }

  private ObjectNode toObject(ResultSet resultSet, int i)
  {
    ObjectNode object = new ObjectNode(null);
    fields.forEach(field -> setField(object, field.name(), get(field, resultSet)));

    return object;
  }

  private void setField(ObjectNode object, String name, JsonNode value)
  {
    if (value != null) object.set(name, value);
  }

  private JsonNode get(Field field, ResultSet resultSet)
  {
    try
    {
      return Fields.get(field, resultSet);
    }
    catch (SQLException e)
    {
      throw new FindException(e);
    }
  }

  private void setValues(PreparedStatement preparedStatement, AtomicInteger index, ObjectNode object) throws SQLException
  {
    try
    {
      fields.forEach(wrapConsumer(setValue(preparedStatement, index, object)));
    }
    catch (WrappedException e)
    {
      throw (SQLException)e.getCause();
    }
  }

  private ConsumerWithThrows<Field, SQLException> setValue(PreparedStatement preparedStatement, AtomicInteger index, ObjectNode object)
  {
    return field -> setValue(preparedStatement, index, field, Fields.get(field, object));
  }

  private void setValue(PreparedStatement preparedStatement, AtomicInteger index, Field field, Object value) throws SQLException
  {
    if (value != null) PreparedStatements.setValue(preparedStatement, index::getAndIncrement, value);
    else preparedStatement.setNull(index.getAndIncrement(), field.column().type());
  }

  private String select(String schema, String table, Collection<Field> fields)
  {
    return format(
      SELECT,
      fields.stream().map(field -> field.column().name()).collect(joining(COLUMN_SEPARATOR)),
      schema,
      table
    );
  }

  private String insert(String schema, String table, Collection<Field> fields)
  {
    return format(
      INSERT,
      schema,
      table,
      fields.stream().map(field -> field.column().name()).collect(joining(COLUMN_SEPARATOR)),
      (COLUMN_PLACEHOLDER + COLUMN_SEPARATOR).repeat(fields.size() - 1) + COLUMN_PLACEHOLDER
    );
  }
}
