package com.bahalla

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions._

object Predicates {

  /** Build a single Spark Column expression from optional filter args. Returns None when no filters
    * are configured (full scan / backup mode). Pure — testable without Kafka or a SparkSession with
    * data.
    */
  def build(cfg: Config): Option[Column] = {
    val parts: Seq[Column] = Seq(
      cfg.keyRegex.map(r => col("key").rlike(r)),
      cfg.valueRegex.map(r => col("value").rlike(r)),
      cfg.partition.map(p => col("partition") === p)
    ).flatten

    if (parts.isEmpty) None else Some(parts.reduceLeft(_ && _))
  }
}
