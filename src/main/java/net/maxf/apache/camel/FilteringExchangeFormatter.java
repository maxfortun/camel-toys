package net.maxf.apache.camel;

import java.util.Map;
import java.util.stream.Collectors;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;

import org.apache.camel.support.processor.DefaultExchangeFormatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FilteringExchangeFormatter extends DefaultExchangeFormatter {
	private static final Logger logger = LogManager.getLogger(FilteringExchangeFormatter.class);

	private String keyFilterPattern = null;
	private String valueFilterPattern = null;
	private String valueTypeFilterPattern = null;

	public void setKeyFilterPattern(String keyFilterPattern) {
		this.keyFilterPattern = keyFilterPattern;
	}

	public String getKeyFilterPattern() {
		return keyFilterPattern;
	}

	public void setValueFilterPattern(String valueFilterPattern) {
		this.valueFilterPattern = valueFilterPattern;
	}

	public String getValueFilterPattern() {
		return valueFilterPattern;
	}

	public void setValueTypeFilterPattern(String valueTypeFilterPattern) {
		this.valueTypeFilterPattern = valueTypeFilterPattern;
	}

	public String getValueTypeFilterPattern() {
		return valueTypeFilterPattern;
	}

	protected Map<String, Object> filterHeaderAndProperties(Map<String, Object> map) {
		if(
			   null == keyFilterPattern
			&& null == valueTypeFilterPattern
			&& null == valueFilterPattern
		) {
			logger.trace("No filter patterns set.");
			return map;
		}

		return map.entrySet()
			.stream()
			.filter(entry -> {

				String key = entry.getKey();
				if(null == key) {
					logger.trace("Null key.");
					return false;
				}

				Object value = entry.getValue();
				if(null == value) {
					logger.trace("Null value for key {}.", key);
					return false;
				}

				if(null != keyFilterPattern && key.matches(keyFilterPattern)) {
					logger.trace("{} matches keyFilterPattern {}.", key, keyFilterPattern);
					return false;
				}

				if(null != valueTypeFilterPattern) {
					String valueType = value.getClass().getName();
					if(valueType.matches(valueTypeFilterPattern)) {
						logger.trace("{} matches valueTypeFilterPattern {}.", valueType, valueTypeFilterPattern);
						return false;
					}
				}

				if(null != valueFilterPattern) {
					String stringValue = (null != value ? value.toString() : "");
					if(stringValue.matches(valueFilterPattern)) {
						logger.trace("{} matches valueFilterPattern {}.", value, valueFilterPattern);
						return false;
					}
				}

				logger.trace("{} ok to trace.", key);

				return true;
			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}

