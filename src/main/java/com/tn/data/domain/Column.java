package com.tn.data.domain;

public record Column(String name, int type, boolean key, boolean nullable, boolean autoIncrement) {}