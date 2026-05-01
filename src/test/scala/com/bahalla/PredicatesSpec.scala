package com.bahalla

import org.scalatest.funsuite.AnyFunSuite

class PredicatesSpec extends AnyFunSuite {

  test("no filters returns None (full scan)") {
    assert(Predicates.build(Config()).isEmpty)
  }

  test("single filter returns Some") {
    assert(Predicates.build(Config(valueRegex = Some("foo"))).isDefined)
    assert(Predicates.build(Config(keyRegex = Some("k.*"))).isDefined)
    assert(Predicates.build(Config(partition = Some(0))).isDefined)
  }

  test("multiple filters AND together") {
    val cfg = Config(keyRegex = Some("k.*"), valueRegex = Some("v.*"), partition = Some(2))
    val sql = Predicates.build(cfg).get.expr.sql
    assert(sql.toLowerCase.contains("rlike"))
    assert(sql.contains("AND"))
    assert(sql.contains("partition"))
  }
}
