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
Here is a use case, we need to perform multiple tasks on a single request.   

The easiest way to code it is:  
```
Request -> Service
                   -> Task 1
                   -> Task 2
                   -> Task 3
Response <- Service
```
The problem with this is approach is that tasks happen sequentially, one after another, and take time of all the tasks combined. (Time of Task 1 + Time of Task 2 + Time of Task 3) 

The efficient way to code it is so that tasks all happen at the same time and when done get aggregated. With this approach the time the request takes is the time of a single longest running task. The way to achieve it:
```
Request -> Service
                   -> Publish to Task start topic
                   <- Subscribe to Task done topic
Response <- Service
```
Have multiple task specific subscribers to the start topic to do multiple tasks at once, pipe their output to an aggregator, send aggregated response to response topic. Send back the response. Save time.   

This is a generic reference implementation on how to achieve it.

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

Run mock work:
```
bin/camel-run.sh MockWork.xml
```

Run the load test:
```
bin/syncasync-test.js 
```

