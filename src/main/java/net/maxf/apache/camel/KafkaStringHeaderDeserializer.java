package net.maxf.apache.camel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Objects;

import org.apache.camel.component.kafka.serde.KafkaHeaderDeserializer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class KafkaStringHeaderDeserializer implements KafkaHeaderDeserializer {
	private static final Logger logger = LogManager.getLogger(KafkaStringHeaderDeserializer.class);

	private Charset charset = StandardCharsets.UTF_8;

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = Objects.requireNonNull(charset);
	}

	@Override
	public Object deserialize(String key, byte[] value) {
		String nextValue = new String(value, charset);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Header %s deserialized to %s", key, nextValue));
		}
		return nextValue;
	}
}
