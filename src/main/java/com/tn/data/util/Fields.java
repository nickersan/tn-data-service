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
import com.fasterxml.jackson.databind.node.TextNode;

import com.tn.data.domain.Column;
import com.tn.data.domain.Field;

public class Fields
{
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String DATA_TYPE = "DATA_TYPE";
  private static final String IS_NULLABLE = "IS_NULLABLE";

  private Fields() {}

  public static Collection<Field> findAll(DataSource dataSource, String schema, String table) throws SQLException
  {
    try (Connection connection = dataSource.getConnection())
    {
      return fields(schema, table, connection.getMetaData());
    }
  }

  public static JsonNode read(Field field, ResultSet resultSet) throws SQLException
  {
    if (field.type() == boolean.class) return read(resultSet.getBoolean(field.column().name()), BooleanNode::valueOf);
    if (field.type() == int.class) return read(resultSet.getInt(field.column().name()), IntNode::valueOf);
    if (field.type() == long.class) return read(resultSet.getLong(field.column().name()), LongNode::valueOf);
    if (field.type() == float.class) return read(resultSet.getFloat(field.column().name()), FloatNode::valueOf);
    if (field.type() == double.class) return read(resultSet.getDouble(field.column().name()), DoubleNode::valueOf);
    if (field.type() == BigDecimal.class) return read(resultSet.getBigDecimal(field.column().name()), DecimalNode::valueOf);
    if (field.type() == String.class) return read(resultSet.getString(field.column().name()), TextNode::valueOf);
    if (field.type() == Date.class) return read(String.valueOf(resultSet.getDate(field.column().name())), TextNode::valueOf);
    if (field.type() == Time.class) return read(String.valueOf(resultSet.getTime(field.column().name())), TextNode::valueOf);
    if (field.type() == Timestamp.class) return read(String.valueOf(resultSet.getTimestamp(field.column().name())), TextNode::valueOf);

    throw new IllegalArgumentException("Unsupported field type: " + field);
  }

  private static <T> JsonNode read(T value, Function<T, JsonNode> mapper)
  {
    return value == null ? null : mapper.apply(value);
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
            new Column(columnName, columnType, keyColumnNames.contains(columnName), resultSet.getBoolean(IS_NULLABLE))
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
