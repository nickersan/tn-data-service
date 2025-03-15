package com.tn.data.repository;

import com.tn.data.domain.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FieldRepositoryIntegrationTest
{
  private static final String SCHEMA = "public";
  private static final String TABLE = "test";

  @Autowired
  DataSource dataSource;
  @Autowired
  FieldRepository fieldRepository;

  @ParameterizedTest
  @ValueSource(
    strings =
    {
      "BIT"
    }
  )
  void shouldFindFieldForBitColumn(String type) throws Exception
  {
    createTable(type);

    Collection<Field> fields = fieldRepository.findForTable(SCHEMA, TABLE);

    assertFalse(fields.isEmpty());
  }

  private void createTable(String type) throws SQLException
  {
    try (
      Connection connection = dataSource.getConnection();
      Statement statement = connection.createStatement()
    )
    {
      statement.execute(
        String.format(
          """
          CREATE TABLE %1$s (
            id INT NOT NULL PRIMARY KEY,
            value_1 %2$s NOT NULL,
            value_2 %2$s NULL
          );
          """,
          TABLE,
          type
        )
      );
    }
  }
}
