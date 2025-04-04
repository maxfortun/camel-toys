# camel-toys

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
Run Kafka. We'll use it to dispatch async work.
```
docker run --rm -p 9092:9092 \
	--env-file src/test/resources/kafka.env \
	--name kafka apache/kafka
```
  
If it is running on a docker host that is not localhost, override with:
```
	-e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.1.123:9092
```

### Run Kafbat, a Kafka console to observe queued up work.
```
docker run --rm -p 8080:8080 \
	--env-file src/test/resources/kafbat.env \
	--name kafbat kafbat/kafka-ui

```

If it is running on a docker host that is not localhost, override with:
```
	-e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=192.168.1.123:9092
```


