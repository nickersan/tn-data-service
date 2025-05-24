package com.tn.service.data.repository;

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
