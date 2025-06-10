package com.tn.service.data.io;

public class InvalidIdException extends RuntimeException
{
  public InvalidIdException(String message)
  {
    super(message);
  }

  public InvalidIdException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
