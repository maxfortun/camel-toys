# camel-toys

## Build for testing
```
mvn clean package dependency:copy-dependencies
```

## FilteringExchangeFormatter
```
   <bean id="exchangeFormatter" class="com.dowjones.artpub.camel.logging.FilteringExchangeFormatter">
        <property name="showExchangeId" value="true" />
        <property name="showProperties" value="true" />
        <property name="showAllProperties" value="true" />
        <property name="showHeaders" value="true" />
        <property name="showBodyType" value="true" />
        <property name="showBody" value="true" />
        <property name="showAll" value="true" />
        <property name="showException" value="true" />
        <property name="showStackTrace" value="true" />
        <property name="showCaughtException" value="true" />
        <property name="showFuture" value="true" />
        <property name="showExchangePattern" value="true" />
        <property name="showCachedStreams" value="true" />
        <property name="showStreams" value="true" />
        <property name="showFiles" value="true" />

        <property name="keyFilterPattern" value="(?i)^(kafka.HEADERS).*" />
    </bean>
    
    <bean id="tracer" class="org.apache.camel.impl.engine.DefaultTracer">
        <property name="exchangeFormatter" ref="exchangeFormatter" />
    </bean>
```

## SyncAsyncGateway
### Kafka
We'll use Kafka to dispatch async work.

Create a network:
```
docker network create kafka
```

Run Kafka:
```
docker run --rm \
	--network kafka \
	-p 0.0.0.0:9092:9092 \
	--env-file src/test/resources/kafka.env \
	--name kafka apache/kafka
```
  
If it is running on a docker host that is not localhost, override advertised PLAINTEXT listener with your own host:
```
	-e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.1.123:9092,DOCKER://kafka:9094
```

### Run Kafbat, a Kafka console to observe queued up work.
```
docker run --rm -p 0.0.0.0:8080:8080 \
	--env-file src/test/resources/kafbat.env \
	--name kafbat kafbat/kafka-ui

```

> You can now access Kafka UI either at http://localhost:8080 or http://192.168.1.123:8080, if your docker host is different.


### Set environment
If your docker host is different, set it first:
```
export KAFKA_BROKERS=192.168.1.123:9092
```

Set the SyncAsyncGateway env:
```
. bin/setenv-syncasync-kafka.sh 
```

Run SyncAsyncGateway:
```
bin/camel-run.sh SyncAsyncGateway.xml
```
