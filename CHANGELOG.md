# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-01

First cut of the search/backup tool — read a Kafka topic with Spark, optionally
filter, write the matches to console / Parquet / JSONL.

### Added

- `KafkaSearch` CLI entrypoint (`com.bahalla.KafkaSearch`) with scopt-based
  argument parsing.
- `Config` case class capturing every CLI flag, with validation
  (`--out parquet|jsonl` requires `--out-path`).
- `PayloadFormat` sealed ADT (`Raw`, `Json`) — extension point for future Avro
  / Protobuf / Schema Registry support.
- `Predicates.build` — pure column-builder that AND-combines optional
  `--key-regex` / `--value-regex` / `--partition` filters; returns `None` for
  full-scan/backup mode.
- `Sink` trait with `ConsoleSink`, `ParquetSink`, `JsonlSink` implementations.
- Time-bounded reads via Spark's `startingTimestamp` / `endingTimestamp`
  (ISO-8601 input, applied uniformly to all partitions).
- Offset-bounded reads via Spark's `startingOffsets` / `endingOffsets`
  (defaults: `earliest` → `latest`).
- SLF4J logging with a bundled `log4j2.properties` (INFO for `com.bahalla`,
  WARN for `org.apache.spark` / `org.apache.kafka`).
- Multi-stage `Dockerfile` (sbt builder → `apache/spark:3.5.3` runtime); container
  args are forwarded to `spark-submit`.
- `Makefile` targets: `up` / `down` / `nuke` / `logs` / `topic` / `produce` /
  `consume` / `compile` / `test` / `assembly` / `clean` / `search` / `backup` /
  `image` / `run-docker`.
- `docker-compose.yaml` with `kafka` (cp-kafka 7.9.2, KRaft, dual-listener) and
  `kafka-ui` (kafbat).
- Tests: `ConfigSpec` (parser + validation), `PredicatesSpec` (filter
  combinations), `KafkaSearchSpec` (decode + filter against a synthetic Kafka
  DataFrame). 14 tests, no real broker required.

### Build

- Scala 2.12.18, Spark 3.5.8, sbt 1.10.11.
- Dependencies: `spark-core` & `spark-sql` (Provided), `spark-sql-kafka-0-10`
  (bundled), `scopt 4.1.0`, `scalatest 3.2.20`.
- Fixed `sbt runMain` to include `Provided` deps (previously only `sbt run` did).
- `assembly / mainClass` set to `com.bahalla.KafkaSearch`.

### Known limitations

- Avro / Schema Registry not yet supported — `--format json` and `--format raw`
  both decode bytes to UTF-8 strings.
- JSONL sink with `--format json` writes values as escaped strings rather than
  nested JSON objects (requires `from_json` with a schema).
- No `--header` filter; Kafka headers need a UDF to be searchable.
- No CI workflow yet.

[Unreleased]: https://example.com/compare/v0.1.0...HEAD
[0.1.0]: https://example.com/releases/tag/v0.1.0
