package com.bahalla

/** Wire format of Kafka message values, selected by the `--format` CLI flag.
  *
  * v1 supports Raw and Json (both decode bytes -> UTF-8 string; the tag is propagated to sinks so
  * JSON values can be rendered as nested objects rather than escaped strings). v2 will add cases
  * like `Avro(schemaRegistryUrl)` and `Protobuf(...)` that decoders and sinks branch on — extending
  * the ADT will surface every site that needs to handle the new format as an exhaustivity warning.
  */
sealed trait PayloadFormat {
  def name: String
}

object PayloadFormat {
  case object Raw  extends PayloadFormat { val name = "raw"  }
  case object Json extends PayloadFormat { val name = "json" }

  val all: Seq[PayloadFormat] = Seq(Raw, Json)

  def parse(s: String): Either[String, PayloadFormat] =
    all
      .find(_.name.equalsIgnoreCase(s))
      .toRight(
        s"unknown payload format '$s' (expected one of: ${all.map(_.name).mkString(", ")})"
      )
}
