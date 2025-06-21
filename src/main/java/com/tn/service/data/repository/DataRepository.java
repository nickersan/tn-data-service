package com.tn.service.data.repository;

import java.util.Collection;
import java.util.Optional;

import com.tn.lang.util.Page;
import com.tn.service.data.domain.Direction;

public interface DataRepository<V, ID>
{
  Optional<V> find(ID identifier) throws FindException;

  Collection<V> findAll(Iterable<String> sort, Direction direction) throws FindException;

  Page<V> findAll(int pageNumber, int pageSize, Iterable<String> sort, Direction direction) throws FindException;

  Collection<V> findAll(Iterable<ID> identifiers) throws FindException;

  Collection<V> findWhere(String query, Iterable<String> sort, Direction direction) throws FindException;

  Page<V> findWhere(String query, int pageNumber, int pageSize, Iterable<String> sort, Direction direction) throws FindException;

  V insert(V value) throws InsertException;

  Collection<V> insertAll(Iterable<V> values) throws InsertException;

  V update(V value) throws UpdateException;

  Collection<V> updateAll(Iterable<V> values) throws UpdateException;

  Optional<V> delete(ID identifier) throws DeleteException;

  Collection<V>  deleteAll(Iterable<ID> identifiers) throws DeleteException;
}
