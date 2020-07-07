#!/bin/bash
export JVM_OPTS
exec java \
    -server -Xmx$JAVA_MAX_HEAP_SPACE -Xms$JAVA_MIN_HEAP_SPACE \
    $JAVA11_ARGS \
    $GC_ARGS \
    $DEBUG_OPTS \
    $JVM_OPTS \
    -Dhost=0.0.0.0 \
    -jar instantmessenger.jar