package com.cloudxplorer.flightservice.domain;

public enum ActorType {
  CUSTOMER,
  CORP;

  public static ActorType fromHeader(String value) {
    if (value == null || value.isBlank()) {
      return CUSTOMER;
    }
    return "corp".equalsIgnoreCase(value) ? CORP : CUSTOMER;
  }
}
