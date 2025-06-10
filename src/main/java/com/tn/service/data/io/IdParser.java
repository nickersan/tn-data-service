package com.tn.service.data.io;

public interface IdParser<K>
{
  K parse(String key) throws InvalidIdException;
}
