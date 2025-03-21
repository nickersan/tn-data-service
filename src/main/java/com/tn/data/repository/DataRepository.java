package com.tn.data.repository;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataRepository
{
  Collection<ObjectNode> findAll() throws FindException;

  Collection<ObjectNode> findFor(String query) throws FindException;

  ObjectNode insert(ObjectNode object) throws InsertException;

  Collection<ObjectNode> insert(Collection<ObjectNode> objects) throws InsertException;
}
