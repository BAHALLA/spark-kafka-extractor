package com.bahalla

import org.apache.spark.sql.{DataFrame, DataFrameReader, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType
import org.slf4j.LoggerFactory

/** CLI tool: distributed parallel search/dump over a Kafka topic.
  *
  *   - No filters + parquet/jsonl output → backup mode.
  *   - Filters + console/jsonl output → debug/grep mode.
  *
  * See `--help` for flags. Pure pieces (`decode`, `Predicates.build`) are unit-tested directly; the
  * spark-submit path is exercised with `make search` against the local compose stack.
  */
object KafkaSearch {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val cfg = Config.parse(args).getOrElse(sys.exit(2))

    val spark = SparkSession
      .builder()
      .appName(s"KafkaSearch-${cfg.topic}")
      .getOrCreate()

    val exitCode =
      try {
        val matches = run(spark, cfg)
        Sink.forConfig(cfg).write(matches, cfg.format)
        0
      } catch {
        case e: Throwable =>
          log.error(s"Search failed: ${e.getMessage}", e)
          2
      } finally {
        spark.stop()
      }

    if (exitCode != 0) sys.exit(exitCode)
  }

  /** Read kafka, decode payload, apply filters. Returns the matching DataFrame. */
  def run(spark: SparkSession, cfg: Config): DataFrame = {
    log.info(
      s"Scanning topic=${cfg.topic} bootstrap=${cfg.bootstrap} format=${cfg.format.name} " +
        s"range=${rangeDescription(cfg)}"
    )

    val raw     = readKafka(spark, cfg)
    val decoded = decode(raw, cfg.format)

    Predicates.build(cfg).fold(decoded)(decoded.filter)
  }

  /** Decode raw kafka rows into key/value strings + metadata. The `fmt` argument is intentionally
    * plumbed through but unused in v1; v2 (Avro/Schema Registry) will branch on it without changing
    * callers.
    */
  def decode(raw: DataFrame, fmt: PayloadFormat): DataFrame = {
    val _ = fmt
    raw.select(
      col("topic"),
      col("partition"),
      col("offset"),
      col("timestamp"),
      col("key").cast(StringType).as("key"),
      col("value").cast(StringType).as("value")
    )
  }

  private def readKafka(spark: SparkSession, cfg: Config): DataFrame = {
    val base: DataFrameReader = spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", cfg.bootstrap)
      .option("subscribe", cfg.topic)

    val ranged = (cfg.fromTs, cfg.toTs) match {
      case (Some(from), Some(to)) =>
        base
          .option("startingTimestamp", parseIso(from).toString)
          .option("endingTimestamp", parseIso(to).toString)
      case (Some(from), None) =>
        base
          .option("startingTimestamp", parseIso(from).toString)
          .option("endingOffsets", cfg.toOffset)
      case (None, Some(to)) =>
        base
          .option("startingOffsets", cfg.fromOffset)
          .option("endingTimestamp", parseIso(to).toString)
      case (None, None) =>
        base
          .option("startingOffsets", cfg.fromOffset)
          .option("endingOffsets", cfg.toOffset)
    }

    ranged.load()
  }

  private def rangeDescription(cfg: Config): String =
    (cfg.fromTs, cfg.toTs) match {
      case (Some(f), Some(t)) => s"ts[$f .. $t]"
      case (Some(f), None)    => s"ts[$f .. ${cfg.toOffset}]"
      case (None, Some(t))    => s"[${cfg.fromOffset} .. ts $t]"
      case (None, None)       => s"[${cfg.fromOffset} .. ${cfg.toOffset}]"
    }

  private def parseIso(iso: String): Long = java.time.Instant.parse(iso).toEpochMilli
}
