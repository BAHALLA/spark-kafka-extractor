# spark-kafka-extractor

A Spark-based distributed CLI tool for parallelizing Kafka topic searches and backups. Designed to handle TB-scale topics where standard GUI tools fail.

**Features:**
- **Search (Grep):** Apply regex, partition, and time range filters. Stream results to console or JSONL.
- **Backup:** Extract full topics or time slices to Parquet for cold storage or replays.

## Quickstart

```bash
# 1. Start local Kafka & UI
make up

# 2. Create demo topic & produce sample JSON data
make topic TOPIC=orders
make produce TOPIC=orders
# >{"order_id":1,"status":"failed"}

# 3. Search the topic
# Note: Use '.' to match quotes in regex to avoid sbt/shell escaping issues
make search TOPIC=orders ARGS='--format json --value-regex ".*status.:.failed.*"'

# 4. Backup to Parquet
make backup TOPIC=orders OUT=/tmp/orders-backup
```
*(Run `make help` for all targets)*

## CLI Reference & Examples

```text
kafka-search [options]
  -b, --bootstrap <host:port>   Kafka brokers (e.g., localhost:9092)
  -t, --topic <name>            Target topic
  --from-ts / --to-ts <iso>     Time range (e.g., 2026-04-30T14:00:00Z)
  --from-offset / --to-offset   Offset range (default: earliest to latest)
  --key-regex / --value-regex   Filter by key/value matching regex
  --partition <int>             Target single partition
  --format raw|json             Payload decoding (default: raw)
  --out console|parquet|jsonl   Output destination (default: console)
  --out-path <path>             Required for parquet/jsonl sinks
  --limit <int>                 Console output limit (default: 50)
```

**Examples via sbt:**
```bash
# Search time range
sbt "runMain com.bahalla.KafkaSearch -b localhost:9092 -t orders --from-ts 2026-05-01T13:00:00Z --value-regex '.*failed.*'"

# Backup daily Parquet
sbt "runMain com.bahalla.KafkaSearch -b localhost:9092 -t orders --out parquet --out-path /var/backups/orders/2026-05-01"
```

## Build & Run

Requires JDK 11+ and sbt 1.x. The build produces a fat-jar (Spark provided, Kafka bundled).

```bash
make compile test assembly
```

**Docker:**
Builds an `apache/spark` container wrapping the assembled jar.
```bash
make image
make run-docker ARGS='-b kafka:19092 -t orders --limit 5'
```

## Roadmap & Known Limitations

- **Formats:** Only `raw` and `json` supported (Avro/Schema Registry planned).
- **JSONL Nesting:** JSON values are written as escaped strings in JSONL output.
- **Filters:** Header filtering and typed SQL filters (via `--json-schema`) are planned.
- **Aggregations:** A `summarize` mode for topic exploration is on the roadmap.
