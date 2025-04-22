package com.tn.data.repository;

public class DeleteException extends RepositoryException
{
  public DeleteException(String message)
  {
    super(message);
  }

  public DeleteException(Throwable cause)
  {
    super(cause);
  }
}
