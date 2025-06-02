package com.tn.service.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.tn.lang.util.Page;
import com.tn.service.data.domain.Direction;

public interface DataRepository<K, V>
{
  Optional<V> find(K key) throws FindException;

  Collection<V> findAll(Collection<String> sort, Direction direction) throws FindException;

  Page<V> findAll(int pageNumber, int pageSize, Collection<String> sort, Direction direction) throws FindException;

  Collection<V> findAll(Iterable<K> keys, Collection<String> sort, Direction direction) throws FindException;

  Collection<V> findWhere(String query, Collection<String> sort, Direction direction) throws FindException;

  Page<V> findWhere(String query, int pageNumber, int pageSize, Collection<String> sort, Direction direction) throws FindException;

  V insert(V value) throws InsertException;

  @SuppressWarnings("unchecked")
  default Collection<V> insertAll(V... values) throws InsertException
  {
    return insertAll(List.of(values));
  }

  Collection<V> insertAll(Iterable<V> values) throws InsertException;

  V update(V value) throws UpdateException;

  @SuppressWarnings("unchecked")
  default Collection<V> updateAll(V... values) throws UpdateException
  {
    return updateAll(List.of(values));
  }

  Collection<V> updateAll(Iterable<V> values) throws UpdateException;

  void delete(K key) throws DeleteException;

  @SuppressWarnings("unchecked")
  default void deleteAll(K... keys) throws DeleteException
  {
    deleteAll(List.of(keys));
  }

  void deleteAll(Iterable<K> keys) throws DeleteException;
}
