package com.tn.service.data.repository;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tn.lang.util.Page;
import com.tn.service.data.domain.Direction;

public interface DataRepository<K, V>
{
  Optional<V> find(K key) throws FindException;

  default Collection<V> findAll()
  {
    return findAll(emptySet(), Direction.ASCENDING);
  }

  Collection<V> findAll(Iterable<String> sort, Direction direction) throws FindException;

  default Page<V> findAll(int pageNumber, int pageSize) throws FindException
  {
    return findAll(pageNumber, pageSize, emptySet(), Direction.ASCENDING);
  }

  Page<V> findAll(int pageNumber, int pageSize, Iterable<String> sort, Direction direction) throws FindException;

  @SuppressWarnings("unchecked")
  default Collection<V> findAll(K... keys)
  {
    return findAll(Set.of(keys));
  }

  Collection<V> findAll(Iterable<K> keys);

  default Collection<V> findWhere(String query) throws FindException
  {
    return findWhere(query, emptySet(), Direction.ASCENDING);
  }

  Collection<V> findWhere(String query, Iterable<String> sort, Direction direction) throws FindException;

  default Page<V> findWhere(String query, int pageNumber, int pageSize) throws FindException
  {
    return findWhere(query, pageNumber, pageSize, emptySet(), Direction.ASCENDING);
  }

  Page<V> findWhere(String query, int pageNumber, int pageSize, Iterable<String> sort, Direction direction) throws FindException;

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
