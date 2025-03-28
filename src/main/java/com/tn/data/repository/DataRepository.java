package com.tn.data.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataRepository
{
  Optional<ObjectNode> find(ObjectNode key) throws FindException;

  Collection<ObjectNode> findAll() throws FindException;

  Map<ObjectNode, ObjectNode> findAll(Collection<ObjectNode> keys) throws FindException;

  Collection<ObjectNode> findFor(String query) throws FindException;

  ObjectNode insert(ObjectNode object) throws InsertException;

  Collection<ObjectNode> insert(Collection<ObjectNode> objects) throws InsertException;

  ObjectNode update(ObjectNode object) throws UpdateException;
}
