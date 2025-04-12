package com.tn.data.io;

public class InvalidKeyException extends RuntimeException
{
  public InvalidKeyException(String message)
  {
    super(message);
  }

  public InvalidKeyException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
