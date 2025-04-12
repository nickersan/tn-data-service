package com.tn.data.io;

public class KeyParseException extends RuntimeException
{
  public KeyParseException(String message)
  {
    super(message);
  }

  public KeyParseException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
