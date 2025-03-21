package com.tn.data.repository;

public abstract class RepositoryException extends RuntimeException
{
  public RepositoryException(Throwable cause)
  {
    super(cause);
  }
}
