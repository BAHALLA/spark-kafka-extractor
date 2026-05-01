package com.bahalla

import org.apache.spark.sql.{DataFrame, SaveMode}
import org.slf4j.LoggerFactory

trait Sink {
  def write(df: DataFrame, fmt: PayloadFormat): Unit
}

object Sink {
  def forConfig(cfg: Config): Sink = cfg.out match {
    case OutputFormat.Console => new ConsoleSink(cfg.limit)
    case OutputFormat.Parquet => new ParquetSink(cfg.outPath.get)
    case OutputFormat.Jsonl   => new JsonlSink(cfg.outPath.get)
  }
}

final class ConsoleSink(limit: Int) extends Sink {
  private val log = LoggerFactory.getLogger(getClass)
  def write(df: DataFrame, fmt: PayloadFormat): Unit = {
    val cached = df.cache()
    val n      = cached.count()
    log.info(s"Matched $n messages (showing up to $limit)")
    cached.limit(limit).show(limit, truncate = false)
  }
}

final class ParquetSink(path: String) extends Sink {
  private val log = LoggerFactory.getLogger(getClass)
  def write(df: DataFrame, fmt: PayloadFormat): Unit = {
    log.info(s"Writing parquet to $path")
    df.write.mode(SaveMode.Overwrite).parquet(path)
  }
}

final class JsonlSink(path: String) extends Sink {
  private val log = LoggerFactory.getLogger(getClass)
  def write(df: DataFrame, fmt: PayloadFormat): Unit = {
    // v1 limitation: JSON values are written as escaped strings (Spark needs a schema to nest them).
    // v2 will use from_json with a user-supplied or inferred schema when fmt == Json.
    log.info(s"Writing jsonl to $path (format=${fmt.name})")
    df.write.mode(SaveMode.Overwrite).json(path)
  }
}
