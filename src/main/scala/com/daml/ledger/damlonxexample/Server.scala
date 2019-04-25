package com.daml.ledger.damlonxexample

object Server extends Greeting with App {
  println(greeting)
}

trait Greeting {
  lazy val greeting: String = "hello"
}
