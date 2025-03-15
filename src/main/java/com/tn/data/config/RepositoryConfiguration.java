package com.tn.data.config;

import com.tn.data.domain.Field;
import com.tn.data.repository.DataRepository;
import com.tn.data.repository.FieldRepository;
import com.tn.data.repository.JdbcDataRepository;
import com.tn.data.repository.JdbcFieldRepository;
import com.tn.query.DefaultQueryParser;
import com.tn.query.ValueMappers;
import com.tn.query.jdbc.JdbcPredicateFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collection;

import static java.util.stream.Collectors.toMap;

@Configuration
class RepositoryConfiguration
{
  DataRepository dataRepository(
    DataSource dataSource,
    Collection<Field> fields
  )
  {
    return new JdbcDataRepository(
      dataSource,
      null,
      null,
      fields,
      new DefaultQueryParser<>(
        new JdbcPredicateFactory(fields.stream().collect(toMap(Field::name, field -> field.column().name()))),
        ValueMappers.forFields(fields.stream().map(field -> new ValueMappers.Field(field.name(), field.type())).toList())
      )
    );
  }

  @Bean
  FieldRepository fieldRepository(DataSource dataSource)
  {
    return new JdbcFieldRepository(dataSource);
  }
}
