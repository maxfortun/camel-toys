
export KAFKA_BROKERS=${KAFKA_BROKERS:-localhost:9092}

export PRODUCER_START="kafka:syncAsyncGate-start?brokers=$KAFKA_BROKERS"
export CONSUMER_START="kafka:syncAsyncGate-start?brokers=$KAFKA_BROKERS&consumersCount=100&groupId=start"

export PRODUCER_DONE="kafka:syncAsyncGate-done?brokers=$KAFKA_BROKERS"
export CONSUMER_DONE="kafka:syncAsyncGate-done?brokers=$KAFKA_BROKERS&consumersCount=100&groupId=done"

export PRODUCER_REPLY_TO="kafka:syncAsyncGate-replyTo?brokers=$KAFKA_BROKERS"
export CONSUMER_REPLY_TO="kafka:syncAsyncGate-replyTo?brokers=$KAFKA_BROKERS&consumersCount=100&groupId=replyTo&headerDeserializer=#kafkaStringHeaderDeserializer"
