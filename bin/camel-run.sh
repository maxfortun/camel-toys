#!/bin/bash -ex

camelContext=$1

if [ -z "$camelContext" ]; then
	echo "$0: <camel-context.xml>"
	exit 1
fi

export NODE_ID=a

java -cp "src/test/resources:src/main/resources:target/*:target/dependency/*" \
	org.apache.camel.spring.Main \
	-ac $camelContext
