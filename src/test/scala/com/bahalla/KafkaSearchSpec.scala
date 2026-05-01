package com.bahalla

import java.sql.Timestamp

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class KafkaSearchSpec extends AnyFunSuite with BeforeAndAfterAll {

  @transient private lazy val spark: SparkSession = {
    val s = SparkSession
      .builder()
      .appName("KafkaSearchSpec")
      .master("local[2]")
      .config("spark.ui.enabled", "false")
      .getOrCreate()
    s
  }

  override def afterAll(): Unit = spark.stop()

  private def syntheticKafka(): DataFrame = {
    import spark.implicits._
    val ts = Timestamp.valueOf("2026-01-01 00:00:00")
    Seq(
      ("alice".getBytes, """{"status":"ok"}""".getBytes, "demo", 0, 1L, ts),
      ("bob".getBytes, """{"status":"failed"}""".getBytes, "demo", 1, 2L, ts),
      ("carol".getBytes, """{"status":"ok"}""".getBytes, "demo", 0, 3L, ts)
    ).toDF("key", "value", "topic", "partition", "offset", "timestamp")
  }

  test("decode casts bytes to UTF-8 strings and preserves metadata") {
    val out = KafkaSearch.decode(syntheticKafka(), PayloadFormat.Raw).collect()
    assert(out.length == 3)
    assert(
      out.map(_.getAs[String]("value")).toSet ==
        Set("""{"status":"ok"}""", """{"status":"failed"}""")
    )
  }

  test("value-regex filter selects matching rows") {
    val decoded  = KafkaSearch.decode(syntheticKafka(), PayloadFormat.Json)
    val cfg      = Config(valueRegex = Some(""".*"failed".*"""))
    val filtered = Predicates.build(cfg).fold(decoded)(decoded.filter).collect()
    assert(filtered.length == 1)
    assert(filtered.head.getAs[String]("key") == "bob")
  }

  test("partition filter selects matching rows") {
    val decoded  = KafkaSearch.decode(syntheticKafka(), PayloadFormat.Raw)
    val cfg      = Config(partition = Some(0))
    val filtered = Predicates.build(cfg).fold(decoded)(decoded.filter).collect()
    assert(filtered.length == 2)
    assert(filtered.map(_.getAs[String]("key")).toSet == Set("alice", "carol"))
  }

  test("combined filters AND together") {
    val decoded  = KafkaSearch.decode(syntheticKafka(), PayloadFormat.Json)
    val cfg      = Config(partition = Some(0), valueRegex = Some(""".*"ok".*"""))
    val filtered = Predicates.build(cfg).fold(decoded)(decoded.filter).collect()
    assert(filtered.length == 2)
  }

  test("no filters returns all rows (backup mode)") {
    val decoded  = KafkaSearch.decode(syntheticKafka(), PayloadFormat.Raw)
    val filtered = Predicates.build(Config()).fold(decoded)(decoded.filter).collect()
    assert(filtered.length == 3)
  }
}
