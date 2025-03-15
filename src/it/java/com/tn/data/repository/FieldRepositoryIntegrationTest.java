package com.tn.data.repository;

import static java.lang.String.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.tn.data.domain.Column;
import com.tn.data.domain.Field;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FieldRepositoryIntegrationTest
{
  // Note: tests use H2 which is case-sensitive when getting metadata.
  private static final String SCHEMA = "PUBLIC";
  private static final String TABLE = "TEST";

  @Autowired
  DataSource dataSource;
  @Autowired
  FieldRepository fieldRepository;


  @ParameterizedTest
  @MethodSource("types")
  @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "DROP TABLE PUBLIC.TEST")
  void shouldFindFields(int columnType, Class<?> fieldType) throws Exception
  {
    createTable(columnType);

    assertEquals(
      List.of(
        new Field("id", int.class, new Column("ID", Types.INTEGER, true, false)),
        new Field("value1", fieldType, new Column("VALUE_1", columnType, false, false)),
        new Field("value2", fieldType, new Column("VALUE_2", columnType, false, true))
      ),
      fieldRepository.findForTable(SCHEMA, TABLE)
    );
  }

  static Stream<Arguments> types()
  {
    // Note: H2 doesn't support BIT or LONGNVARCHAR.
    return Stream.of(
      Arguments.of(Types.BOOLEAN, boolean.class),
      Arguments.of(Types.TINYINT, int.class),
      Arguments.of(Types.SMALLINT, int.class),
      Arguments.of(Types.INTEGER, int.class),
      Arguments.of(Types.BIGINT, long.class),
      Arguments.of(Types.FLOAT, float.class),
      Arguments.of(Types.DOUBLE, double.class),
      Arguments.of(Types.DECIMAL, BigDecimal.class),
      Arguments.of(Types.CHAR, String.class),
      Arguments.of(Types.VARCHAR, String.class),
      Arguments.of(Types.DATE, Date.class),
      Arguments.of(Types.TIME, Time.class),
      Arguments.of(Types.TIMESTAMP, Timestamp.class)
    );
  }

  private void createTable(int type) throws SQLException, IllegalAccessException
  {
    try (
      Connection connection = dataSource.getConnection();
      Statement statement = connection.createStatement()
    )
    {
      String typeName = typeName(type);

      statement.executeUpdate(
        format(
          """
          CREATE TABLE %1$s.%2$s (
            id INT NOT NULL PRIMARY KEY,
            value_1 %3$s NOT NULL,
            value_2 %3$s NULL
          );
          """,
          SCHEMA,
          TABLE,
          typeName
        )
      );
    }
  }

  private String typeName(int type) throws IllegalAccessException
  {
    for (var field : Types.class.getDeclaredFields())
    {
      if (field.getInt(null) == type) return field.getName();
    }

    throw new IllegalArgumentException("Unsupported data type: " + type);
  }
}
