package com.tn.data.controller;

public class BadRequestException extends RuntimeException
{
  public BadRequestException(String message)
  {
    super(message);
  }
}
