package com.tn.service.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.tn.lang.util.Page;

public interface DataRepository<K, V>
{
  Optional<V> find(K key) throws FindException;

  Collection<V> findAll() throws FindException;

  Page<V> findAll(int pageNumber, int pageSize) throws FindException;

  @SuppressWarnings("unchecked")
  default Collection<V> findAll(K... keys) throws FindException
  {
    return findAll(List.of(keys));
  }

  Collection<V> findAll(Iterable<K> keys) throws FindException;

  Collection<V> findFor(String query) throws FindException;

  Page<V> findFor(String query, int pageNumber, int pageSize) throws FindException;

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
