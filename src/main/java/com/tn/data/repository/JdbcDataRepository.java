package com.tn.data.repository;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import static com.google.common.collect.Lists.partition;

import static com.tn.lang.util.function.Lambdas.wrapConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

  private final JdbcTemplate jdbcTemplate;
  private final Collection<Field> fields;
  private final Collection<Field> autoIncrementFields;
  private final Collection<Field> insertableFields;
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
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.fields = fields;
    this.queryParser = queryParser;

    this.autoIncrementFields = fields.stream().filter(field -> field.column().autoIncrement()).toList();
    this.insertableFields = fields.stream().filter(field -> !field.column().autoIncrement()).toList();

    this.select = select(schema, table, fields);
    this.insert = insert(schema, table, insertableFields);
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
      return jdbcTemplate.query(select, this::toObject);
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
      return jdbcTemplate.query(
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
      KeyHolder keyHolder = new GeneratedKeyHolder();

      jdbcTemplate.update(
        connection ->
        {
          PreparedStatement preparedStatement = connection.prepareStatement(
            insert,
            autoIncrementFields.isEmpty() ? PreparedStatement.NO_GENERATED_KEYS : PreparedStatement.RETURN_GENERATED_KEYS
          );
          setValues(preparedStatement, new AtomicInteger(1), object);
          return preparedStatement;
        },
        keyHolder
      );

      if (autoIncrementFields.isEmpty()) return object;

      return withIdentifiers(object, keyHolder.getKeyList().getFirst());
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
    if (!(objects instanceof List)) return insert(List.copyOf(objects));

    try
    {
      Collection<ObjectNode> persistedObjects = new ArrayList<>();

      for (List<ObjectNode> batch : partition((List<ObjectNode>)objects, batchSize))
      {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.batchUpdate(
          connection -> connection.prepareStatement(
            insert,
            autoIncrementFields.isEmpty() ? PreparedStatement.NO_GENERATED_KEYS : PreparedStatement.RETURN_GENERATED_KEYS
          ),
          batchPreparedStatementSetter(batch),
          keyHolder
        );

        persistedObjects.addAll(autoIncrementFields.isEmpty() ? batch : withIdentifiers(batch, keyHolder.getKeyList()));
      }

      return persistedObjects;
    }
    catch (DataAccessException e)
    {
      throw new InsertException(e.getCause());
    }
  }

  private BatchPreparedStatementSetter batchPreparedStatementSetter(List<ObjectNode> objects)
  {
    return new BatchPreparedStatementSetter()
    {
      @Override
      public void setValues(@Nonnull PreparedStatement preparedStatement, int index) throws SQLException
      {
        JdbcDataRepository.this.setValues(preparedStatement, new AtomicInteger(1), objects.get(index));
      }

      @Override
      public int getBatchSize()
      {
        return objects.size();
      }
    };
  }

  private List<ObjectNode> withIdentifiers(List<ObjectNode> objects, List<Map<String, Object>> identifiers)
  {
    if (identifiers.size() != objects.size()) throw new InsertException("Identifier mismatch after insert");

    return IntStream.range(0, objects.size())
      .mapToObj(i -> withIdentifiers(objects.get(i), identifiers.get(i)))
      .toList();
  }

  private ObjectNode withIdentifiers(ObjectNode object, Map<String, Object> identifiers)
  {
    ObjectNode objectWithIdentifier = new ObjectNode(null);

    for (Field field : fields)
    {
      if (field.column().autoIncrement()) Fields.with(objectWithIdentifier, field, identifiers.get(field.column().name()));
      else if (object.has(field.name())) objectWithIdentifier.set(field.name(), object.get(field.name()));
    }

    return objectWithIdentifier;
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
      insertableFields.forEach(wrapConsumer(setValue(preparedStatement, index, object)));
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

  private String insert(String schema, String table, Collection<Field> insertableFields)
  {
    return format(
      INSERT,
      schema,
      table,
      insertableFields.stream().map(field -> field.column().name()).collect(joining(COLUMN_SEPARATOR)),
      (COLUMN_PLACEHOLDER + COLUMN_SEPARATOR).repeat(insertableFields.size() - 1) + COLUMN_PLACEHOLDER
    );
  }
}
