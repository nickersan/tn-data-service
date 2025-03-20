package com.tn.data.repository;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataRepository
{
  Collection<ObjectNode> findAll() throws FindException;

  Collection<ObjectNode> findFor(String query) throws FindException;
}
