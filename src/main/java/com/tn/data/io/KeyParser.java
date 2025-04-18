package com.tn.data.io;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface KeyParser
{
  ObjectNode parse(String key) throws InvalidKeyException;
}
