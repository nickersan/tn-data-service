package com.tn.service.data.repository;

public class UpdateException extends RepositoryException
{
  public UpdateException(String message)
  {
    super(message);
  }

  public UpdateException(Throwable cause)
  {
    super(cause);
  }
}
