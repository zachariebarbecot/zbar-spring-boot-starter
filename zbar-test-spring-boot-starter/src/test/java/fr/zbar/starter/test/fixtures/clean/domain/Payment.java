package fr.zbar.starter.test.fixtures.clean.domain;

public sealed interface Payment {

  record Paid(Order order) implements Payment {}

  record Pending(Order order) implements Payment {}
}
