# Convenience targets for the spark-kafka-extractor project.
# Run `make help` for a list.

SHELL := /bin/bash

PROJECT      := spark-kafka-extractor
VERSION      := 0.1.0-SNAPSHOT
JAR          := target/scala-2.12/$(PROJECT)-$(VERSION).jar
IMAGE        := $(PROJECT):latest

KAFKA_BOOTSTRAP_HOST := localhost:9092
KAFKA_BOOTSTRAP_NET  := kafka:19092
TOPIC                ?= demo
MAIN_CLASS           ?= com.bahalla.KafkaSearch
ARGS                 ?=

COMPOSE := docker compose

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help.
	@awk 'BEGIN {FS = ":.*##"; printf "Targets:\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

# ---- Kafka stack ----

.PHONY: up
up: ## Start Kafka + kafka-ui in the background.
	$(COMPOSE) up -d

.PHONY: down
down: ## Stop Kafka stack (keeps the data volume).
	$(COMPOSE) down

.PHONY: nuke
nuke: ## Stop Kafka stack and remove the data volume.
	$(COMPOSE) down -v

.PHONY: logs
logs: ## Tail Kafka broker logs.
	$(COMPOSE) logs -f kafka

.PHONY: topic
topic: ## Create TOPIC=$(TOPIC) (idempotent). Override with TOPIC=foo.
	$(COMPOSE) exec kafka kafka-topics \
		--bootstrap-server $(KAFKA_BOOTSTRAP_NET) \
		--create --if-not-exists \
		--topic $(TOPIC) --partitions 3 --replication-factor 1

.PHONY: produce
produce: ## Open an interactive console producer on $(TOPIC).
	$(COMPOSE) exec -it kafka kafka-console-producer \
		--bootstrap-server $(KAFKA_BOOTSTRAP_NET) --topic $(TOPIC)

.PHONY: consume
consume: ## Tail messages from $(TOPIC) from the beginning.
	$(COMPOSE) exec kafka kafka-console-consumer \
		--bootstrap-server $(KAFKA_BOOTSTRAP_NET) --topic $(TOPIC) --from-beginning

# ---- sbt build ----

.PHONY: fmt
fmt: ## Run scalafmt to format the codebase.
	sbt scalafmtAll scalafmtSbt

.PHONY: fmtcheck
fmtcheck: ## Check if the codebase is formatted.
	sbt scalafmtCheckAll scalafmtSbtCheck

.PHONY: compile
compile: ## sbt compile.
	sbt compile

.PHONY: test
test: ## Run scalatest.
	sbt test

.PHONY: assembly
assembly: ## Build the fat jar at $(JAR).
	sbt assembly

.PHONY: clean
clean: ## sbt clean.
	sbt clean

# ---- Run KafkaSearch locally via sbt (against host Kafka on localhost:9092) ----

.PHONY: search
search: ## Run a search. Pass ARGS='--key-regex foo --value-regex bar'. Default TOPIC=$(TOPIC).
	sbt "runMain $(MAIN_CLASS) -b $(KAFKA_BOOTSTRAP_HOST) -t $(TOPIC) $(ARGS)"

.PHONY: backup
backup: ## Full topic dump to parquet. Required: OUT=/path/to/dir.
	@if [ -z "$(OUT)" ]; then echo "OUT is required (e.g. make backup OUT=/tmp/dump TOPIC=demo)"; exit 2; fi
	sbt "runMain $(MAIN_CLASS) -b $(KAFKA_BOOTSTRAP_HOST) -t $(TOPIC) --out parquet --out-path $(OUT)"

# ---- Docker image ----

.PHONY: image
image: ## Build the runtime Docker image ($(IMAGE)).
	docker build -t $(IMAGE) .

.PHONY: run-docker
run-docker: ## Run KafkaSearch in a container on the compose network. Pass ARGS='...'.
	docker run --rm --network=$$(basename $$PWD)_default \
		$(IMAGE) -b $(KAFKA_BOOTSTRAP_NET) -t $(TOPIC) $(ARGS)
