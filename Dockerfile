# Multi-stage build: compile a fat jar with sbt, then run it on a slim Spark image.

# ---- Stage 1: build the assembly jar ----
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.10_7_1.10.11_2.12.18 AS build

WORKDIR /build

# Cache dependencies first by copying only build definitions.
COPY project/build.properties project/plugins.sbt ./project/
COPY build.sbt ./
RUN sbt update

# Now copy sources and build the fat jar.
COPY src ./src
RUN sbt clean assembly

# ---- Stage 2: runtime image with Spark + spark-submit ----
FROM apache/spark:3.5.3-scala2.12-java17-python3-ubuntu

USER root
WORKDIR /opt/app

COPY --from=build /build/target/scala-2.13/spark-kafka-extractor-0.1.0-SNAPSHOT.jar /opt/app/app.jar

ENV SPARK_MASTER=local[*]

# `$@` forwards container args to spark-submit, so:
#   docker run image -b kafka:19092 -t demo --value-regex '.*"failed".*'
ENTRYPOINT ["/bin/bash", "-c", \
  "exec /opt/spark/bin/spark-submit \
    --master $SPARK_MASTER \
    --class com.bahalla.KafkaSearch \
    /opt/app/app.jar \"$@\"", \
  "--"]
CMD ["--help"]
