package com.tn.data.util;

import static com.tn.lang.Characters.UNDERSCORE;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.tn.data.domain.Column;
import com.tn.data.domain.Field;

public class Fields
{
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String DATA_TYPE = "DATA_TYPE";
  private static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
  private static final String IS_NULLABLE = "IS_NULLABLE";

  private Fields() {}

  public static Collection<Field> findAll(DataSource dataSource, String schema, String table) throws SQLException
  {
    try (Connection connection = dataSource.getConnection())
    {
      return fields(schema, table, connection.getMetaData());
    }
  }

  public static JsonNode get(Field field, ResultSet resultSet) throws SQLException
  {
    if (field.type() == boolean.class) return get(resultSet, resultSet.getBoolean(field.column().name()), BooleanNode::valueOf);
    if (field.type() == int.class) return get(resultSet, resultSet.getInt(field.column().name()), IntNode::valueOf);
    if (field.type() == long.class) return get(resultSet, resultSet.getLong(field.column().name()), LongNode::valueOf);
    if (field.type() == float.class) return get(resultSet, resultSet.getFloat(field.column().name()), FloatNode::valueOf);
    if (field.type() == double.class) return get(resultSet, resultSet.getDouble(field.column().name()), DoubleNode::valueOf);
    if (field.type() == BigDecimal.class) return get(resultSet, resultSet.getBigDecimal(field.column().name()), DecimalNode::valueOf);
    if (field.type() == String.class) return get(resultSet, resultSet.getString(field.column().name()), TextNode::valueOf);
    if (field.type() == Date.class) return get(resultSet, String.valueOf(resultSet.getDate(field.column().name())), TextNode::valueOf);
    if (field.type() == Time.class) return get(resultSet, String.valueOf(resultSet.getTime(field.column().name())), TextNode::valueOf);
    if (field.type() == Timestamp.class) return get(resultSet, String.valueOf(resultSet.getTimestamp(field.column().name())), TextNode::valueOf);

    throw new IllegalArgumentException("Unsupported field type: " + field);
  }

  public static Object get(Field field, ObjectNode object)
  {
    JsonNode value = object.get(field.name());

    if (value == null || value.isNull())
    {
      if (field.column().nullable()) return null;
      else throw new IllegalStateException("Field " + field.name() + " does not allow nulls");
    }

    if (value.isBoolean() && field.type() == boolean.class) return value.asBoolean();
    if (value.isInt() && field.type() == int.class) return value.asInt();
    if (value.isLong() && field.type() == long.class) return value.asLong();
    if (value.isFloat() && field.type() == float.class) return value.floatValue();
    if (value.isDouble() && field.type() == double.class) return value.doubleValue();
    if (value.isBigDecimal() && field.type() == BigDecimal.class) return value.decimalValue();
    if (value.isTextual() && field.type() == String.class) return value.textValue();
    if (value.isTextual() && field.type() == Date.class) return Date.valueOf(value.textValue());
    if (value.isTextual() && field.type() == Time.class) return Time.valueOf(value.textValue());
    if (value.isTextual() && field.type() == Timestamp.class) return Timestamp.valueOf(value.textValue());

    throw new IllegalStateException("Unsupported value/field type: " + value + "/" + field);
  }

  public static void with(ObjectNode objectNode, Field field, Object value)
  {
    if (value instanceof Boolean && field.type() == boolean.class) objectNode.set(field.name(), BooleanNode.valueOf((Boolean)value));
    else if (value instanceof Integer && field.type() == int.class) objectNode.set(field.name(), IntNode.valueOf((Integer)value));
    else if (value instanceof Long && field.type() == long.class) objectNode.set(field.name(), LongNode.valueOf((Long)value));
    else if (value instanceof Float && field.type() == float.class) objectNode.set(field.name(), FloatNode.valueOf((Float)value));
    else if (value instanceof Double && field.type() == double.class) objectNode.set(field.name(), DoubleNode.valueOf((Double)value));
    else if (value instanceof BigDecimal && field.type() == BigDecimal.class) objectNode.set(field.name(), DecimalNode.valueOf((BigDecimal)value));
    else if (value instanceof String && field.type() == String.class) objectNode.set(field.name(), TextNode.valueOf((String)value));
    else if (value instanceof Date && field.type() == Date.class) objectNode.set(field.name(), TextNode.valueOf(String.valueOf(value)));
    else if (value instanceof Time && field.type() == Time.class) objectNode.set(field.name(), TextNode.valueOf(String.valueOf(value)));
    else if (value instanceof Timestamp && field.type() == Timestamp.class) objectNode.set(field.name(), TextNode.valueOf(String.valueOf(value)));

    if (!objectNode.has(field.name())) throw new IllegalArgumentException("Unsupported field type: " + field);
  }

  private static <T> JsonNode get(ResultSet resultSet, T value, Function<T, JsonNode> mapper) throws SQLException
  {
    return resultSet.wasNull() || value == null ? null : mapper.apply(value);
  }

  private static Collection<Field> fields(String schema, String table, DatabaseMetaData databaseMetaData) throws SQLException
  {
    Collection<String> keyColumnNames = keyColumnNames(schema, table, databaseMetaData);
    Collection<Field> fields = new ArrayList<>();

    try (ResultSet resultSet = databaseMetaData.getColumns(null, schema, table, null))
    {
      while (resultSet.next())
      {
        String columnName = resultSet.getString(COLUMN_NAME);
        int columnType = resultSet.getInt(DATA_TYPE);
        fields.add(
          new Field(
            toFieldName(columnName),
            toFieldType(columnType),
            new Column(
              columnName,
              columnType,
              keyColumnNames.contains(columnName),
              resultSet.getBoolean(IS_NULLABLE),
              resultSet.getBoolean(IS_AUTOINCREMENT)
            )
          )
        );
      }
    }

    return fields;
  }

  private static Set<String> keyColumnNames(String schema, String table, DatabaseMetaData databaseMetaData) throws SQLException
  {
    Set<String> keyColumnNames = new HashSet<>();

    try (ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, schema, table))
    {
      while (resultSet.next()) keyColumnNames.add(resultSet.getString(COLUMN_NAME));
    }

    return keyColumnNames;
  }

  private static String toFieldName(String columnName)
  {
    StringBuilder fieldName = new StringBuilder();
    fieldName.append(Character.toLowerCase(columnName.charAt(0)));

    for (int i = 1; i < columnName.length(); i++)
    {
      char c = columnName.charAt(i);
      if (c == UNDERSCORE)
      {
        i++;
        if (i < columnName.length()) fieldName.append(Character.toUpperCase(columnName.charAt(i)));
      }
      else
      {
        fieldName.append(Character.toLowerCase(c));
      }
    }

    return fieldName.toString();
  }

  private static Class<?> toFieldType(int dataType)
  {
    return switch (dataType)
    {
      case Types.BIT, Types.BOOLEAN -> boolean.class;
      case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> int.class;
      case Types.BIGINT -> long.class;
      case Types.FLOAT -> float.class;
      case Types.DOUBLE -> double.class;
      case Types.DECIMAL -> BigDecimal.class;
      case Types.CHAR, Types.VARCHAR, Types.LONGNVARCHAR -> String.class;
      case Types.DATE -> Date.class;
      case Types.TIME -> Time.class;
      case Types.TIMESTAMP -> Timestamp.class;
      default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
    };
  }
}
