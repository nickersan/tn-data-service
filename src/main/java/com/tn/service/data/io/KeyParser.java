package com.tn.service.data.io;

public interface KeyParser<K>
{
  K parse(String key) throws InvalidKeyException;
}
