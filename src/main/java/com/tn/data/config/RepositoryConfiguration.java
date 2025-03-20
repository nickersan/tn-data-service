package com.tn.data.config;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.tn.data.domain.Field;
import com.tn.data.repository.DataRepository;
import com.tn.data.repository.FieldRepository;
import com.tn.data.repository.JdbcDataRepository;
import com.tn.data.repository.JdbcFieldRepository;
import com.tn.query.DefaultQueryParser;
import com.tn.query.ValueMappers;
import com.tn.query.jdbc.JdbcPredicateFactory;

@Configuration
class RepositoryConfiguration
{
  @Bean
  @Lazy
  DataRepository dataRepository(
    FieldRepository fieldRepository,
    DataSource dataSource,
    @Value("${tn.data.schema}")
    String schema,
    @Value("${tn.data.table}")
    String table
  )
  {
    Collection<Field> fields = fieldRepository.findForTable(schema, table);
    if (fields.isEmpty()) throw new IllegalStateException("No such table: " + schema + "." + table);

    return new JdbcDataRepository(
      dataSource,
      schema,
      table,
      fields,
      new DefaultQueryParser<>(
        new JdbcPredicateFactory(fields.stream().collect(toMap(Field::name, field -> field.column().name()))),
        ValueMappers.forFields(fields.stream().map(field -> new ValueMappers.Field(field.name(), field.type())).toList())
      )
    );
  }

  @Bean
  @Lazy
  FieldRepository fieldRepository(DataSource dataSource)
  {
    return new JdbcFieldRepository(dataSource);
  }
}
