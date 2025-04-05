package com.tn.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.tn.lang.util.Page;

public interface DataRepository
{
  Optional<ObjectNode> find(ObjectNode key) throws FindException;

  Collection<ObjectNode> findAll() throws FindException;

  Page<ObjectNode> findAll(int pageNumber, int pageSize) throws FindException;

  default Collection<ObjectNode> findAll(ObjectNode... keys) throws FindException
  {
    return findAll(List.of(keys));
  }

  Collection<ObjectNode> findAll(Iterable<ObjectNode> keys) throws FindException;

  Collection<ObjectNode> findFor(String query) throws FindException;

  Page<ObjectNode> findFor(String query, int pageNumber, int pageSize) throws FindException;

  ObjectNode insert(ObjectNode object) throws InsertException;

  default Collection<ObjectNode> insertAll(ObjectNode... objects) throws InsertException
  {
    return insertAll(List.of(objects));
  }

  Collection<ObjectNode> insertAll(Iterable<ObjectNode> objects) throws InsertException;

  ObjectNode update(ObjectNode object) throws UpdateException;

  default Collection<ObjectNode> updateAll(ObjectNode... objects) throws UpdateException
  {
    return updateAll(List.of(objects));
  }

  Collection<ObjectNode> updateAll(Iterable<ObjectNode> objects) throws UpdateException;

  void delete(ObjectNode key) throws DeleteException;

  default void deleteAll(ObjectNode... keys) throws DeleteException
  {
    deleteAll(List.of(keys));
  }

  void deleteAll(Iterable<ObjectNode> keys) throws DeleteException;
}
