package com.tn.data.repository;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;

import static com.google.common.collect.Lists.partition;

import static com.tn.lang.Iterables.isEmpty;
import static com.tn.lang.Iterables.toList;
import static com.tn.lang.util.function.Lambdas.unwrapException;
import static com.tn.lang.util.function.Lambdas.wrapConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private static final String FIELD_PLACEHOLDER = "%s = ?";
  private static final String LOGICAL_AND = " AND ";
  private static final String LOGICAL_OR = " OR ";
  private static final String PARENTHESIS = "(%s)";
  private static final String SELECT = "SELECT %s FROM %s.%s";
  private static final String INSERT = "INSERT INTO %s.%s(%s) VALUES (%s)";
  private static final String UPDATE = "UPDATE %s.%s SET %s WHERE %s";
  private static final String WHERE = "%s WHERE %s";

  private final JdbcTemplate jdbcTemplate;
  private final Collection<Field> fields;
  private final Collection<Field> autoIncrementFields;
  private final Collection<Field> keyFields;
  private final Collection<Field> mutableFields;
  private final QueryParser<JdbcPredicate> queryParser;
  private final String selectSql;
  private final String insertSql;
  private final String schema;
  private final String table;
  private final String keyPredicate;

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
    this.schema = schema;
    this.table = table;
    this.fields = fields;
    this.queryParser = queryParser;

    this.autoIncrementFields = fields.stream().filter(field -> field.column().autoIncrement()).toList();
    this.keyFields = fields.stream().filter(field -> field.column().key()).toList();
    this.mutableFields = fields.stream().filter(field -> !field.column().key()).toList();

    this.keyPredicate = keyFields.stream().map(field -> FIELD_PLACEHOLDER.formatted(field.column().name())).collect(joining(LOGICAL_AND));

    this.selectSql = selectSql(schema, table, fields);
    this.insertSql = insertSql(schema, table, fields);
  }

  public JdbcDataRepository withBatchSize(int batchSize)
  {
    this.batchSize = batchSize;
    return this;
  }

  @Override
  public Optional<ObjectNode> find(ObjectNode key) throws FindException
  {
    try
    {
      return jdbcTemplate.query(
        WHERE.formatted(selectSql, keyPredicate),
        preparedStatement -> setValues(preparedStatement, parameterIndex(), key, keyFields),
        this::object
      ).stream().findFirst();
    }
    catch (DataAccessException e)
    {
      throw new FindException(e.getCause());
    }
  }

  @Override
  public Collection<ObjectNode> findAll() throws FindException
  {
    try
    {
      return jdbcTemplate.query(selectSql, this::object);
    }
    catch (DataAccessException e)
    {
      throw new FindException(e.getCause());
    }
  }

  @Override
  public Collection<ObjectNode> findAll(Iterable<ObjectNode> keys) throws FindException
  {
    if (isEmpty(keys)) return emptyList();

    try
    {
      AtomicInteger parameterIndex = parameterIndex();
      //noinspection SqlSourceToSinkFlow
      return jdbcTemplate.query(
        WHERE.formatted(selectSql, stream(keys.spliterator(), false).map(key -> format(PARENTHESIS, keyPredicate)).collect(joining(LOGICAL_OR))),
        preparedStatement -> keys.forEach(wrapConsumer(key -> setValues(preparedStatement, parameterIndex, key, keyFields))),
        this::object
      );
    }
    catch (DataAccessException e)
    {
      throw new FindException(e.getCause());
    }
    catch (WrappedException e)
    {
      Throwable unwrapped = unwrapException(e);
      if (unwrapped instanceof RuntimeException) throw (RuntimeException)unwrapped;
      else throw new FindException(unwrapped);
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
        WHERE.formatted(selectSql, predicate.toSql()),
        predicate::setValues,
        this::object
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
            insertSql,
            autoIncrementFields.isEmpty() ? PreparedStatement.NO_GENERATED_KEYS : PreparedStatement.RETURN_GENERATED_KEYS
          );
          AtomicInteger parameterIndex = parameterIndex();
          setValues(preparedStatement, parameterIndex, object, keyFields.stream().filter(keyField -> !keyField.column().autoIncrement()).toList());
          setValues(preparedStatement, parameterIndex, object, mutableFields);
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
  public Collection<ObjectNode> insertAll(Iterable<ObjectNode> objects) throws InsertException
  {
    if (isEmpty(objects)) return emptyList();
    if (!(objects instanceof List)) return insertAll(toList(objects));

    try
    {
      List<Field> insertableKeyFields = keyFields.stream().filter(keyField -> !keyField.column().autoIncrement()).toList();
      Collection<ObjectNode> persistedObjects = new ArrayList<>();

      partition((List<ObjectNode>)objects, batchSize).forEach(
        batch ->
        {
          KeyHolder keyHolder = new GeneratedKeyHolder();

          jdbcTemplate.batchUpdate(
            connection -> connection.prepareStatement(
              insertSql,
              autoIncrementFields.isEmpty() ? PreparedStatement.NO_GENERATED_KEYS : PreparedStatement.RETURN_GENERATED_KEYS
            ),
            batchPreparedStatementSetter(batch, insertableKeyFields, mutableFields),
            keyHolder
          );

          persistedObjects.addAll(autoIncrementFields.isEmpty() ? batch : withIdentifiers(batch, keyHolder.getKeyList()));
        }
      );

      return persistedObjects;
    }
    catch (DataAccessException e)
    {
      throw new InsertException(e.getCause());
    }
  }

  @Override
  @Transactional
  public ObjectNode update(ObjectNode object) throws UpdateException
  {
    try
    {
      Collection<Field> mutableFields = mutableFields(object);
      if (mutableFields.isEmpty()) throw new UpdateException("Unrecognized object: " + object);

      jdbcTemplate.update(
        updateSql(mutableFields),
        preparedStatement ->
        {
          AtomicInteger parameterIndex = parameterIndex();
          setValues(preparedStatement, parameterIndex, object, mutableFields);
          setValues(preparedStatement, parameterIndex, object, keyFields);
        }
      );

      return find(object).orElseThrow(() -> new UpdateException("Failed to find object after update: " + object));
    }
    catch (DataAccessException e)
    {
      throw new UpdateException(e.getCause());
    }
  }

  @Override
  @Transactional
  public Collection<ObjectNode> updateAll(Iterable<ObjectNode> objects) throws UpdateException
  {
    if (isEmpty(objects)) return emptyList();

    stream(objects.spliterator(), false).collect(groupingBy(this::mutableFields)).forEach(
      (mutableFields, objectsForMutableFields) ->
      {
        if (mutableFields.isEmpty()) throw new UpdateException("Unrecognized objects: " + objectsForMutableFields);
        String updateSql = updateSql(mutableFields);

        partition(objectsForMutableFields, batchSize).forEach(
          batch -> jdbcTemplate.batchUpdate(updateSql, batchPreparedStatementSetter(batch, mutableFields, keyFields))
        );
      }
    );

    return findAll(objects);
  }

  @SafeVarargs
  private BatchPreparedStatementSetter batchPreparedStatementSetter(List<ObjectNode> objects, Collection<Field>... fieldGroups)
  {
    return new BatchPreparedStatementSetter()
    {
      @Override
      public void setValues(@Nonnull PreparedStatement preparedStatement, int index) throws SQLException
      {
        AtomicInteger parameterIndex = parameterIndex();
        for (Collection<Field> fieldGroup : fieldGroups) JdbcDataRepository.this.setValues(preparedStatement, parameterIndex, objects.get(index), fieldGroup);
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

  private ObjectNode object(ResultSet resultSet, int i)
  {
    ObjectNode object = new ObjectNode(null);
    keyFields.forEach(field -> setField(object, field.name(), get(field, resultSet)));
    mutableFields.forEach(field -> setField(object, field.name(), get(field, resultSet)));

    return object;
  }

  private Collection<Field> mutableFields(ObjectNode object)
  {
    return mutableFields.stream().filter(field -> object.has(field.name())).toList();
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

  private void setValues(PreparedStatement preparedStatement, AtomicInteger index, ObjectNode object, Collection<Field> fields) throws SQLException
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

  private String selectSql(String schema, String table, Collection<Field> fields)
  {
    return format(
      SELECT,
      fields.stream().map(field -> field.column().name()).collect(joining(COLUMN_SEPARATOR)),
      schema,
      table
    );
  }

  private String insertSql(String schema, String table, Collection<Field> fields)
  {
    Collection<Field> insertableFields = fields.stream().filter(field -> !field.column().autoIncrement()).toList();

    return format(
      INSERT,
      schema,
      table,
      insertableFields.stream().map(insertableField -> insertableField.column().name()).collect(joining(COLUMN_SEPARATOR)),
      (COLUMN_PLACEHOLDER + COLUMN_SEPARATOR).repeat(insertableFields.size() - 1) + COLUMN_PLACEHOLDER
    );
  }

  private String updateSql(Collection<Field> updatableFields)
  {
    return format(
      UPDATE,
      schema,
      table,
      updatableFields.stream().map(field -> format(FIELD_PLACEHOLDER, field.column().name())).collect(joining(COLUMN_SEPARATOR)),
      keyFields.stream().map(field -> format(FIELD_PLACEHOLDER, field.column().name())).collect(joining(LOGICAL_AND))
    );
  }

  private AtomicInteger parameterIndex()
  {
    return new AtomicInteger(1);
  }
}
