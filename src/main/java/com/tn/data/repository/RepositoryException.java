package com.tn.data.repository;

public abstract class RepositoryException extends RuntimeException
{
  public RepositoryException(String message)
  {
    super(message);
  }

  public RepositoryException(Throwable cause)
  {
    super(cause);
  }
}
