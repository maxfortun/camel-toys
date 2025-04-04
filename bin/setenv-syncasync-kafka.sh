
export KAFKA_BROKERS=${KAFKA_BROKERS:-localhost:9092}

export PRODUCER_OPTS="brokers=$KAFKA_BROKERS"
export CONSUMER_OPTS="brokers=$KAFKA_BROKERS"

export START="kafka:syncAsyncGate-start"
export DONE="kafka:syncAsyncGate-done"
export REPLY_TO="kafka:syncAsyncGate-replyTo"
