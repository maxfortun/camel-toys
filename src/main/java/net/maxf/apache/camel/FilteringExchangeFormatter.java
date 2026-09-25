package net.maxf.apache.camel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;

import org.apache.camel.support.processor.DefaultExchangeFormatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ExchangeFormatter with key/value/type filtering of properties, variables and headers.
 *
 * In deltaMode (on by default) the first trace of an exchange is a full dump; subsequent traces only log properties, variables
 * and headers that changed or were removed since the previous trace of that exchange, e.g.:
 *   Exchange[Id: X, Changed Headers: {a=2}, Removed Headers: [b]]
 *   Exchange[Id: X, Unchanged]
 * The last traced state is kept in an exchange property, so exchanges copied from another one (split,
 * multicast, ...) start from the state of their origin and log "From: <origin id>" instead of a full dump.
 * Exchanges with an exception are always fully dumped.
 *
 * With a lineSeparator set, every line break in the output (e.g. stack traces, multiline values) is replaced
 * by it, so each trace is a single log line.
 */
public class FilteringExchangeFormatter extends DefaultExchangeFormatter {
	private static final Logger logger = LogManager.getLogger(FilteringExchangeFormatter.class);

	/** Default name of the exchange property holding the last traced state. */
	public static final String DEFAULT_SNAPSHOT_PROPERTY = "net.maxf.traceDeltaSnapshot";

	private static final Pattern LINE_BREAK = Pattern.compile("\r\n|\r|\n");

	private String keyFilterPattern = null;
	private String valueFilterPattern = null;
	private String valueTypeFilterPattern = null;
	private boolean deltaMode = true;
	private String snapshotProperty = DEFAULT_SNAPSHOT_PROPERTY;
	private String lineSeparator = null;

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

	public void setDeltaMode(boolean deltaMode) {
		this.deltaMode = deltaMode;
	}

	public boolean isDeltaMode() {
		return deltaMode;
	}

	/** Name of the exchange property holding the last traced state in deltaMode. Never traced itself. */
	public void setSnapshotProperty(String snapshotProperty) {
		if(null == snapshotProperty || snapshotProperty.isBlank()) {
			throw new IllegalArgumentException("snapshotProperty must not be blank");
		}
		this.snapshotProperty = snapshotProperty;
	}

	public String getSnapshotProperty() {
		return snapshotProperty;
	}

	/** Replacement for line breaks, may be multicharacter. Null or empty keeps line breaks as they are. */
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	public String getLineSeparator() {
		return lineSeparator;
	}

	protected Map<String, Object> filterHeaderAndProperties(Map<String, Object> map) {
		if(map.containsKey(snapshotProperty)) {
			map = new HashMap<>(map);
			map.remove(snapshotProperty);
		}

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

	@Override
	public String format(Exchange exchange) {
		String out = formatTrace(exchange);
		if(null == lineSeparator || lineSeparator.isEmpty() || null == out) {
			return out;
		}
		return LINE_BREAK.matcher(out).replaceAll(Matcher.quoteReplacement(lineSeparator));
	}

	private String formatTrace(Exchange exchange) {
		if(!deltaMode) {
			return super.format(exchange);
		}

		Snapshot previous = exchange.getProperty(snapshotProperty, Snapshot.class);
		Snapshot current  = snapshot(exchange);
		exchange.setProperty(snapshotProperty, current);

		if(null == previous || null != exchange.getException()) {
			logger.trace("Full trace of {}.", exchange.getExchangeId());
			return super.format(exchange);
		}

		return formatDelta(previous, current);
	}

	private Snapshot snapshot(Exchange exchange) {
		Map<String, Object> properties = Collections.emptyMap();
		if(isShowAll() || isShowAllProperties()) {
			properties = exchange.getAllProperties();
		} else if(isShowProperties()) {
			properties = exchange.getProperties();
		}

		Map<String, Object> variables = Collections.emptyMap();
		if((isShowAll() || isShowVariables()) && exchange.hasVariables()) {
			variables = exchange.getVariables();
		}

		Map<String, Object> headers = Collections.emptyMap();
		if(isShowAll() || isShowHeaders()) {
			headers = exchange.getIn().getHeaders();
		}

		return new Snapshot(exchange.getExchangeId(), stringify(properties), stringify(variables), stringify(headers));
	}

	// Values are captured as strings so objects mutated in place are detected as changed.
	private Map<String, String> stringify(Map<String, Object> map) {
		Map<String, String> result = new TreeMap<>();
		for(Map.Entry<String, Object> entry : filterHeaderAndProperties(map).entrySet()) {
			result.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return Collections.unmodifiableMap(result);
	}

	private String formatDelta(Snapshot previous, Snapshot current) {
		StringBuilder sb = new StringBuilder("Exchange[Id: ").append(current.exchangeId);
		if(!current.exchangeId.equals(previous.exchangeId)) {
			sb.append(", From: ").append(previous.exchangeId);
		}

		int length = sb.length();
		appendChanges(sb, "Properties", previous.properties, current.properties);
		appendChanges(sb, "Variables", previous.variables, current.variables);
		appendChanges(sb, "Headers", previous.headers, current.headers);
		if(sb.length() == length) {
			sb.append(", Unchanged");
		}
		sb.append(']');

		int maxChars = getMaxChars();
		if(maxChars > 0 && sb.length() > maxChars) {
			return sb.substring(0, maxChars) + "...";
		}
		return sb.toString();
	}

	private static void appendChanges(StringBuilder sb, String label, Map<String, String> previous, Map<String, String> current) {
		Map<String, String> changed = new TreeMap<>();
		for(Map.Entry<String, String> entry : current.entrySet()) {
			if(!Objects.equals(previous.get(entry.getKey()), entry.getValue())) {
				changed.put(entry.getKey(), entry.getValue());
			}
		}

		List<String> removed = new ArrayList<>();
		for(String key : previous.keySet()) {
			if(!current.containsKey(key)) {
				removed.add(key);
			}
		}

		if(!changed.isEmpty()) {
			sb.append(", Changed ").append(label).append(": ").append(changed);
		}
		if(!removed.isEmpty()) {
			sb.append(", Removed ").append(label).append(": ").append(removed);
		}
	}

	/** Immutable, so it is safe to share between an exchange and its copies. */
	static final class Snapshot {
		final String exchangeId;
		final Map<String, String> properties;
		final Map<String, String> variables;
		final Map<String, String> headers;

		Snapshot(String exchangeId, Map<String, String> properties, Map<String, String> variables, Map<String, String> headers) {
			this.exchangeId = exchangeId;
			this.properties = properties;
			this.variables  = variables;
			this.headers    = headers;
		}

		@Override
		public String toString() {
			return "TraceSnapshot[" + exchangeId + "]";
		}
	}
}
