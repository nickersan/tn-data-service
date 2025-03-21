package com.tn.data.repository;

public class InsertException extends RepositoryException
{
  public InsertException(String message)
  {
    super(message);
  }

  public InsertException(Throwable cause)
  {
    super(cause);
  }
}
