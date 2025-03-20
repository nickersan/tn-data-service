package com.tn.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.tn.lang.util.function.Lambdas.unwrapException;
import static com.tn.lang.util.function.Lambdas.wrapConsumer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.tn.lang.util.function.WrappedException;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  properties =
  {
    "tn.data.schema=PUBLIC",
    "tn.data.table=TEST",
  }
)
@Sql(
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
  statements = """
    CREATE TABLE PUBLIC.TEST (
      id              INT              NOT NULL PRIMARY KEY,
      boolean_value   BOOLEAN          NULL,
      integer_value   INTEGER          NOT NULL,
      long_value      LONG             NOT NULL,
      float_value     FLOAT            NOT NULL,
      double_value    DOUBLE PRECISION NOT NULL,
      decimal_value   DECIMAL(3, 2)    NOT NULL,
      string_value    VARCHAR(10)      NOT NULL,
      date_value      DATE             NOT NULL,
      time_value      TIME             NOT NULL,
      timestamp_value TIMESTAMP        NOT NULL
    );
  """
)
@Sql(
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS,
  statements = "DROP TABLE PUBLIC.TEST"
)
class JdbcDataRepositoryIntegrationTest
{
  private static final String INSERT = """
    INSERT INTO PUBLIC.TEST (
      id,
      boolean_value,
      integer_value,
      long_value,
      float_value,
      double_value,
      decimal_value,
      string_value,
      date_value,
      time_value,
      timestamp_value
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  """;

  @Autowired
  DataSource dataSource;
  @Autowired
  DataRepository dataRepository;

  @AfterEach
  void deleteAll() throws SQLException
  {
    try (
      Connection connection = dataSource.getConnection();
      Statement statement = connection.createStatement()
    )
    {
      //noinspection SqlNoDataSourceInspection
      statement.executeUpdate("DELETE FROM PUBLIC.TEST");
    }
  }

  @Test
  void shouldFindAll() throws Exception
  {
    ObjectNode object1 = insert(objectNode(1, true, 10, 11, 1.23F, 2.34, BigDecimal.valueOf(3.45), "T1"));
    ObjectNode object2 = insert(objectNode(2, false, 11, 12, 2.23F, 3.34, BigDecimal.valueOf(4.45), "T2"));
    ObjectNode object3 = insert(objectNode(3, true, 12, 13, 3.23F, 4.34, BigDecimal.valueOf(5.45), "T3"));

    assertEquals(List.of(object1, object2, object3), dataRepository.findAll());
  }

  @ParameterizedTest
  @MethodSource("findForObjectNodes")
  void shouldFindFor(String query, ObjectNode objectNode, List<ObjectNode> objectNodes) throws Exception
  {
    try
    {
      objectNodes.forEach(wrapConsumer(this::insert));

      assertEquals(List.of(objectNode), dataRepository.findFor(query));
    }
    catch (WrappedException e)
    {
      throw (Exception)unwrapException(e);
    }
  }

  static Stream<Arguments> findForObjectNodes()
  {
    LocalDateTime now = LocalDateTime.now();

    ObjectNode object1 = objectNode(1, true, 10, 11, 1.23F, 2.34, BigDecimal.valueOf(3.45), "T1", now.minusDays(1).minusMinutes(1));
    ObjectNode object2 = objectNode(2, false, 11, 12, 2.23F, 3.34, BigDecimal.valueOf(4.45), "T2", now);
    ObjectNode object3 = objectNode(3, null, 12, 13, 3.23F, 4.34, BigDecimal.valueOf(5.45), "T3", now.plusDays(1).plusMinutes(1));

    List<ObjectNode> objects = List.of(object1, object2, object3);

    return objects.stream()
      .flatMap(JdbcDataRepositoryIntegrationTest::queryArguments)
      .peek(arguments -> arguments.add(objects))
      .map(arguments -> Arguments.of(arguments.toArray()));
  }

  private static Stream<List<Object>> queryArguments(ObjectNode objectNode)
  {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(objectNode.fieldNames(), Spliterator.ORDERED), false)
      .map(fieldName -> fieldName + "=" + (objectNode.has(fieldName) ? objectNode.get(fieldName).asText() : "null"))
      .map(query -> new ArrayList<>(List.of(query, objectNode)));
  }

  private static ObjectNode objectNode(
    int id,
    Boolean booleanValue,
    int integerValue,
    long longValue,
    float floatValue,
    double doubleValue,
    BigDecimal decimalValue,
    String stringValue
  )
  {
    return objectNode(
      id,
      booleanValue,
      integerValue,
      longValue,
      floatValue,
      doubleValue,
      decimalValue,
      stringValue,
      LocalDateTime.now()
    );
  }

  private static ObjectNode objectNode(
    int id,
    Boolean booleanValue,
    int integerValue,
    long longValue,
    float floatValue,
    double doubleValue,
    BigDecimal decimalValue,
    String stringValue,
    LocalDateTime localDateTime
  )
  {

    Date dateValue = Date.valueOf(localDateTime.toLocalDate());
    Time timeValue = Time.valueOf(localDateTime.toLocalTime());
    Timestamp timestampValue = Timestamp.valueOf(localDateTime.withNano(0));

    ObjectNode objectNode = new ObjectNode(null);
    objectNode.set("id", IntNode.valueOf(id));
    if (booleanValue != null) objectNode.set("booleanValue", BooleanNode.valueOf(booleanValue));
    objectNode.set("integerValue", IntNode.valueOf(integerValue));
    objectNode.set("longValue", LongNode.valueOf(longValue));
    objectNode.set("floatValue", FloatNode.valueOf(floatValue));
    objectNode.set("doubleValue", DoubleNode.valueOf(doubleValue));
    objectNode.set("decimalValue", DecimalNode.valueOf(decimalValue));
    objectNode.set("stringValue", TextNode.valueOf(stringValue));
    objectNode.set("dateValue", TextNode.valueOf(dateValue.toString()));
    objectNode.set("timeValue", TextNode.valueOf(timeValue.toString()));
    objectNode.set("timestampValue", TextNode.valueOf(timestampValue.toString()));

    return objectNode;
  }

  private ObjectNode insert(ObjectNode objectNode) throws SQLException
  {

    try (
      Connection connection = dataSource.getConnection();
      PreparedStatement preparedStatement = connection.prepareStatement(INSERT);
    )
    {
      preparedStatement.setInt(1, objectNode.get("id").asInt());
      if (objectNode.has("booleanValue")) preparedStatement.setBoolean(2, objectNode.get("booleanValue").asBoolean());
      else preparedStatement.setNull(2, Types.BOOLEAN);
      preparedStatement.setInt(3, objectNode.get("integerValue").asInt());
      preparedStatement.setLong(4, objectNode.get("longValue").asLong());
      preparedStatement.setFloat(5, objectNode.get("floatValue").floatValue());
      preparedStatement.setDouble(6, objectNode.get("doubleValue").doubleValue());
      preparedStatement.setBigDecimal(7, objectNode.get("decimalValue").decimalValue());
      preparedStatement.setString(8, objectNode.get("stringValue").asText());
      preparedStatement.setDate(9, Date.valueOf(objectNode.get("dateValue").asText()));
      preparedStatement.setTime(10, Time.valueOf(objectNode.get("timeValue").asText()));
      preparedStatement.setTimestamp(11, Timestamp.valueOf(objectNode.get("timestampValue").asText()));
      preparedStatement.execute();
    }

    return objectNode;
  }
}
