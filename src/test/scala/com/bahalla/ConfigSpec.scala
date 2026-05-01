package com.bahalla

import org.scalatest.funsuite.AnyFunSuite

class ConfigSpec extends AnyFunSuite {

  test("parses minimal required args") {
    val c = Config.parse(Array("-b", "kafka:19092", "-t", "demo")).get
    assert(c.bootstrap == "kafka:19092")
    assert(c.topic == "demo")
    assert(c.format == PayloadFormat.Raw)
    assert(c.out == OutputFormat.Console)
    assert(c.fromOffset == "earliest" && c.toOffset == "latest")
  }

  test("missing required args returns None") {
    assert(Config.parse(Array("-b", "k:9092")).isEmpty)
    assert(Config.parse(Array("-t", "demo")).isEmpty)
  }

  test("parses payload format") {
    assert(
      Config
        .parse(Array("-b", "k:9092", "-t", "x", "--format", "json"))
        .get
        .format == PayloadFormat.Json
    )
    assert(
      Config
        .parse(Array("-b", "k:9092", "-t", "x", "--format", "RAW"))
        .get
        .format == PayloadFormat.Raw
    )
  }

  test("rejects unknown format") {
    assert(Config.parse(Array("-b", "k:9092", "-t", "x", "--format", "yaml")).isEmpty)
  }

  test("parquet/jsonl output requires --out-path") {
    assert(Config.parse(Array("-b", "k:9092", "-t", "x", "--out", "parquet")).isEmpty)
    assert(Config.parse(Array("-b", "k:9092", "-t", "x", "--out", "jsonl")).isEmpty)
    assert(
      Config
        .parse(Array("-b", "k:9092", "-t", "x", "--out", "parquet", "--out-path", "/tmp/out"))
        .isDefined
    )
  }

  test("parses filters and time range") {
    val c = Config
      .parse(
        Array(
          "-b",
          "k:9092",
          "-t",
          "x",
          "--key-regex",
          "k.*",
          "--value-regex",
          "v.*",
          "--partition",
          "3",
          "--from-ts",
          "2026-04-30T14:00:00Z",
          "--to-ts",
          "2026-04-30T15:00:00Z",
          "--limit",
          "100"
        )
      )
      .get
    assert(c.keyRegex.contains("k.*"))
    assert(c.valueRegex.contains("v.*"))
    assert(c.partition.contains(3))
    assert(c.fromTs.contains("2026-04-30T14:00:00Z"))
    assert(c.toTs.contains("2026-04-30T15:00:00Z"))
    assert(c.limit == 100)
  }
}
