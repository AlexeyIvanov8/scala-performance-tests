package com.skn

/**
  * Model for tests
  */
package object model {
  sealed trait ViewItem

  case class Client(name: String, age: Int, balance: BigDecimal, services: Seq[Service]) extends ViewItem
  case class Service(description: String, attributes: Seq[Attribute]) extends ViewItem
  case class Attribute(name: String, value: Any) extends ViewItem
}
