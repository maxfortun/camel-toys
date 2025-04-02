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
