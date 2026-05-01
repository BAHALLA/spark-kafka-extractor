# Project Overview
`spark-kafka-extractor` is a distributed CLI tool built on Apache Spark and Scala for parallelizing searches and taking backups of large-scale Kafka topics. It provides a functional core (parsing and predicate logic) wrapped by a Spark DataFrame I/O shell. 

**Core Technologies:**
- **Language:** Scala 2.13
- **Framework:** Apache Spark 3.5.x (`spark-core`, `spark-sql`, `spark-sql-kafka-0-10`)
- **Build Tool:** sbt 1.x
- **CLI Parsing:** scopt
- **Formatting:** scalafmt

## Architecture
The application adheres strongly to functional and SOLID principles:
- **`KafkaSearch.scala`:** The entrypoint. Handles the impure side effects (reading from the Kafka broker and executing the Spark physical plan).
- **`Config.scala`:** Immutable configuration case class and `scopt` parser definitions.
- **`Predicates.scala`:** Pure functions that translate the CLI configuration into Spark SQL `Column` filters.
- **`Sinks.scala`:** A polymorphic trait pattern handling the output formatting (`ConsoleSink`, `ParquetSink`, `JsonlSink`).

## Building and Running
The repository relies on a `Makefile` to orchestrate `sbt` and Docker commands.

**Local Development (sbt):**
- Compile code: `make compile`
- Run unit tests: `make test`
- Format code: `sbt scalafmtAll scalafmtSbt`
- Build a fat jar: `make assembly`

**Execution & Integration:**
- Start Kafka/UI stack: `make up`
- Run the tool locally: `make search TOPIC=<topic> ARGS='<args>'`
- Build Docker image: `make image` (builds an `apache/spark` runtime container)

## Development Conventions
- **Code Style:** Scala code must be formatted using `scalafmt` (`sbt scalafmtAll`). The configuration `.scalafmt.conf` enforces 100 max columns and alignment.
- **Testing:** 
  - **Unit Tests:** Pure functions (decoding, predicates, config parsing) are unit-tested without a real Kafka broker (`src/test/scala/com/bahalla/`).
  - **E2E Tests:** Integration flows are tested against the local `docker-compose` stack.
- **Dependency Management:** Spark core dependencies (`spark-core`, `spark-sql`) are marked as `Provided` to prevent binary conflicts when deploying the fat jar to a managed cluster (like Dataproc or an `apache/spark` container). The Kafka connector is explicitly bundled.
- **Simplicity (YAGNI/KISS):** Do not introduce abstractions for features not yet required. For example, JSON output is currently written as escaped strings rather than relying on heavy schema inference.
