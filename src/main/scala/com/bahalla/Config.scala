package com.bahalla

import scopt.OParser

sealed trait OutputFormat { def name: String }

object OutputFormat {
  case object Console extends OutputFormat { val name = "console" }
  case object Parquet extends OutputFormat { val name = "parquet" }
  case object Jsonl   extends OutputFormat { val name = "jsonl"   }

  val all: Seq[OutputFormat] = Seq(Console, Parquet, Jsonl)

  def parse(s: String): Either[String, OutputFormat] =
    all
      .find(_.name.equalsIgnoreCase(s))
      .toRight(
        s"unknown output '$s' (expected one of: ${all.map(_.name).mkString(", ")})"
      )
}

final case class Config(
    bootstrap: String = "",
    topic: String = "",
    fromTs: Option[String] = None,
    toTs: Option[String] = None,
    fromOffset: String = "earliest",
    toOffset: String = "latest",
    keyRegex: Option[String] = None,
    valueRegex: Option[String] = None,
    partition: Option[Int] = None,
    format: PayloadFormat = PayloadFormat.Raw,
    out: OutputFormat = OutputFormat.Console,
    outPath: Option[String] = None,
    limit: Int = 50
)

object Config {
  private val builder = OParser.builder[Config]

  private val parser: OParser[Unit, Config] = {
    import builder._
    OParser.sequence(
      programName("kafka-search"),
      head("kafka-search", "0.1.0"),
      opt[String]('b', "bootstrap")
        .required()
        .valueName("<host:port>")
        .action((x, c) => c.copy(bootstrap = x))
        .text("Kafka bootstrap servers (e.g. kafka:19092)"),
      opt[String]('t', "topic")
        .required()
        .valueName("<name>")
        .action((x, c) => c.copy(topic = x))
        .text("Topic to scan"),
      note("\n[range] timestamp options override offset options when both are set"),
      opt[String]("from-ts")
        .valueName("<iso8601>")
        .action((x, c) => c.copy(fromTs = Some(x)))
        .text("Start ISO-8601 timestamp (e.g. 2026-04-30T14:00:00Z)"),
      opt[String]("to-ts")
        .valueName("<iso8601>")
        .action((x, c) => c.copy(toTs = Some(x)))
        .text("End ISO-8601 timestamp"),
      opt[String]("from-offset")
        .action((x, c) => c.copy(fromOffset = x))
        .text("Starting offset spec (default: earliest)"),
      opt[String]("to-offset")
        .action((x, c) => c.copy(toOffset = x))
        .text("Ending offset spec (default: latest)"),
      note("\n[filters] all combined with AND; omit all for a full scan / backup"),
      opt[String]("key-regex")
        .valueName("<regex>")
        .action((x, c) => c.copy(keyRegex = Some(x)))
        .text("Keep rows whose key matches this regex"),
      opt[String]("value-regex")
        .valueName("<regex>")
        .action((x, c) => c.copy(valueRegex = Some(x)))
        .text("Keep rows whose value matches this regex"),
      opt[Int]("partition")
        .action((x, c) => c.copy(partition = Some(x)))
        .text("Keep rows from a single partition"),
      note("\n[decoding & output]"),
      opt[String]("format")
        .valueName(PayloadFormat.all.map(_.name).mkString("|"))
        .validate { s =>
          PayloadFormat.parse(s) match {
            case Right(_)  => success
            case Left(msg) => failure(msg)
          }
        }
        .action((x, c) => c.copy(format = PayloadFormat.parse(x).toOption.get))
        .text("Payload wire format (default: raw)"),
      opt[String]("out")
        .valueName(OutputFormat.all.map(_.name).mkString("|"))
        .validate { s =>
          OutputFormat.parse(s) match {
            case Right(_)  => success
            case Left(msg) => failure(msg)
          }
        }
        .action((x, c) => c.copy(out = OutputFormat.parse(x).toOption.get))
        .text("Output sink (default: console)"),
      opt[String]("out-path")
        .valueName("<path>")
        .action((x, c) => c.copy(outPath = Some(x)))
        .text("Output directory (required for parquet/jsonl)"),
      opt[Int]("limit")
        .action((x, c) => c.copy(limit = x))
        .text("Max rows to print on console (default: 50)"),
      help("help"),
      checkConfig { c =>
        if (c.out != OutputFormat.Console && c.outPath.isEmpty)
          failure("--out-path is required when --out is parquet or jsonl")
        else
          success
      }
    )
  }

  def parse(args: Array[String]): Option[Config] =
    OParser.parse(parser, args, Config())
}
