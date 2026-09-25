package net.maxf.apache.camel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultTracer;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.ExchangeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilteringExchangeFormatterTest {

	private CamelContext context;
	private FilteringExchangeFormatter formatter;

	@BeforeEach
	public void setUp() {
		context = new DefaultCamelContext();
		formatter = new FilteringExchangeFormatter();
		formatter.setShowExchangeId(true);
		formatter.setShowAllProperties(true);
		formatter.setShowVariables(true);
		formatter.setShowHeaders(true);
		formatter.setShowBody(false);
		formatter.setShowBodyType(false);
		formatter.setShowException(true);
		formatter.setKeyFilterPattern("(?i)^(.*authorization)$");
		formatter.setDeltaMode(true);
	}

	@AfterEach
	public void tearDown() {
		context.stop();
	}

	@Test
	public void deltaModeIsOnByDefault() {
		assertTrue(new FilteringExchangeFormatter().isDeltaMode());
	}

	@Test
	public void fullDumpWhenDeltaModeOff() {
		formatter.setDeltaMode(false);
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("a", "1");

		formatter.format(exchange);
		String second = formatter.format(exchange);

		assertTrue(second.contains("Headers: {a=1}"), second);
		assertNull(exchange.getProperty(formatter.getSnapshotProperty()));
	}

	@Test
	public void snapshotPropertyIsConfigurable() {
		assertEquals(FilteringExchangeFormatter.DEFAULT_SNAPSHOT_PROPERTY, formatter.getSnapshotProperty());
		assertThrows(IllegalArgumentException.class, () -> formatter.setSnapshotProperty(" "));

		formatter.setSnapshotProperty("my.snapshot");
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("a", "1");

		String first = formatter.format(exchange);
		exchange.getIn().setHeader("a", "2");
		String second = formatter.format(exchange);

		assertNotNull(exchange.getProperty("my.snapshot"));
		assertNull(exchange.getProperty(FilteringExchangeFormatter.DEFAULT_SNAPSHOT_PROPERTY));
		assertFalse(first.contains("my.snapshot"), first);
		assertEquals("Exchange[Id: " + exchange.getExchangeId() + ", Changed Headers: {a=2}]", second);
	}

	@Test
	public void firstTraceIsFullThenOnlyChanges() {
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("a", "1");
		exchange.getIn().setHeader("b", "2");
		exchange.setProperty("p", "x");
		exchange.setVariable("v", "1");

		String first = formatter.format(exchange);
		assertTrue(first.contains("Headers: {a=1, b=2}"), first);
		assertTrue(first.contains("p=x"), first);
		assertTrue(first.contains("v=1"), first);

		assertEquals("Exchange[Id: " + exchange.getExchangeId() + ", Unchanged]", formatter.format(exchange));

		exchange.getIn().setHeader("a", "changed");
		exchange.getIn().removeHeader("b");
		exchange.getIn().setHeader("c", "3");
		exchange.removeProperty("p");
		exchange.setVariable("v", "2");
		assertEquals(
			"Exchange[Id: " + exchange.getExchangeId()
				+ ", Removed Properties: [p]"
				+ ", Changed Variables: {v=2}"
				+ ", Changed Headers: {a=changed, c=3}, Removed Headers: [b]]",
			formatter.format(exchange));
	}

	@Test
	public void unchangedTraceIsSkippedOnceUnlessTraceUnchanged() {
		assertFalse(new FilteringExchangeFormatter().isTraceUnchanged());
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("a", "1");

		formatter.format(exchange);
		assertFalse(formatter.isTraceSkipped());

		assertTrue(formatter.format(exchange).endsWith(", Unchanged]"));
		assertTrue(formatter.isTraceSkipped());
		assertFalse(formatter.isTraceSkipped());

		exchange.getIn().setHeader("a", "2");
		formatter.format(exchange);
		assertFalse(formatter.isTraceSkipped());

		formatter.setTraceUnchanged(true);
		formatter.format(exchange);
		assertFalse(formatter.isTraceSkipped());
	}

	@Test
	public void filteringTracerDropsUnchangedTraces() throws Exception {
		List<String> traces = traceRoute();
		String all = String.join("\n", traces);
		assertFalse(traces.isEmpty(), all);
		assertTrue(traces.stream().noneMatch(t -> t.contains("Unchanged")), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Headers: {stage=one}")), all);

		formatter.setTraceUnchanged(true);
		context = new DefaultCamelContext();
		List<String> withUnchanged = traceRoute();
		assertTrue(withUnchanged.stream().anyMatch(t -> t.contains("Unchanged")), String.join("\n", withUnchanged));
		assertTrue(withUnchanged.size() > traces.size());
	}

	private List<String> traceRoute() throws Exception {
		List<String> traces = new ArrayList<>();
		FilteringTracer tracer = new FilteringTracer() {
			@Override
			protected void writeTrace(String out, Object node) {
				traces.add(out);
			}
		};
		tracer.setExchangeFormatter(formatter);
		context.setTracing(true);
		context.setTracer(tracer);
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:start").routeId("start")
					.setHeader("stage", constant("one"))
					.log("no change")
					.log("still no change");
			}
		});
		context.start();
		context.createProducerTemplate().sendBody("direct:start", "x");
		context.stop();
		return traces;
	}

	@Test
	public void snapshotAndFilteredKeysAreNeverTraced() {
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("Authorization", "Bearer secret");

		String first = formatter.format(exchange);
		exchange.getIn().setHeader("sourceEnvAuthorization", "Bearer other-secret");
		exchange.getIn().setHeader("a", "1");
		String second = formatter.format(exchange);

		for(String trace : Arrays.asList(first, second)) {
			assertFalse(trace.contains(formatter.getSnapshotProperty()), trace);
			assertFalse(trace.contains("TraceSnapshot"), trace);
			assertFalse(trace.contains("secret"), trace);
		}
		assertEquals("Exchange[Id: " + exchange.getExchangeId() + ", Changed Headers: {a=1}]", second);
	}

	@Test
	public void valuesMutatedInPlaceAreDetected() {
		Exchange exchange = new DefaultExchange(context);
		StringBuilder mutable = new StringBuilder("before");
		exchange.getIn().setHeader("m", mutable);

		formatter.format(exchange);
		mutable.replace(0, mutable.length(), "after");

		assertEquals("Exchange[Id: " + exchange.getExchangeId() + ", Changed Headers: {m=after}]", formatter.format(exchange));
	}

	@Test
	public void copiedExchangeContinuesFromOrigin() {
		Exchange parent = new DefaultExchange(context);
		parent.getIn().setHeader("a", "1");
		formatter.format(parent);

		Exchange child = ExchangeHelper.createCorrelatedCopy(parent, false);
		child.getIn().setHeader("b", "2");

		String trace = formatter.format(child);
		assertTrue(trace.startsWith("Exchange[Id: " + child.getExchangeId() + ", From: " + parent.getExchangeId()), trace);
		assertTrue(trace.contains("Changed Headers: {b=2}"), trace);
		assertFalse(trace.contains("a=1"), trace);
	}

	@Test
	public void exceptionIsAlwaysFullDump() {
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("a", "1");
		formatter.format(exchange);

		exchange.setException(new IllegalStateException("boom"));
		String trace = formatter.format(exchange);

		assertTrue(trace.contains("Headers: {a=1}"), trace);
		assertTrue(trace.contains("boom"), trace);
	}

	@Test
	public void deltaLineHonoursMaxChars() {
		formatter.setMaxChars(40);
		Exchange exchange = new DefaultExchange(context);
		formatter.format(exchange);
		exchange.getIn().setHeader("big", "0123456789012345678901234567890123456789");

		String trace = formatter.format(exchange);
		assertEquals(43, trace.length());
		assertTrue(trace.endsWith("..."), trace);
	}

	@Test
	public void lineBreaksKeptByDefault() {
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("m", "line1\nline2");

		assertTrue(formatter.format(exchange).contains("line1\nline2"));
		formatter.setLineSeparator("");
		exchange.getIn().setHeader("m", "line1\nline3");
		assertTrue(formatter.format(exchange).contains("line1\nline3"));
	}

	@Test
	public void lineBreaksReplacedByMulticharacterSeparator() {
		formatter.setLineSeparator(" \\n$1 | ");
		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setHeader("m", "a\nb\r\nc\rd");

		String first = formatter.format(exchange);
		assertTrue(first.contains("m=a \\n$1 | b \\n$1 | c \\n$1 | d"), first);

		exchange.getIn().setHeader("m", "e\n\nf");
		assertEquals("Exchange[Id: " + exchange.getExchangeId() + ", Changed Headers: {m=e \\n$1 |  \\n$1 | f}]", formatter.format(exchange));
	}

	@Test
	public void stackTraceCollapsedToSingleLine() {
		formatter.setShowStackTrace(true);
		formatter.setLineSeparator(" | ");
		Exchange exchange = new DefaultExchange(context);
		exchange.setException(new IllegalStateException("boom"));

		String trace = formatter.format(exchange);
		assertFalse(trace.contains("\n") || trace.contains("\r"), trace);
		assertTrue(trace.contains("boom | \tat "), trace);
	}

	@Test
	public void tracedRouteWithSplitLogsDeltas() throws Exception {
		List<String> traces = new ArrayList<>();
		DefaultTracer tracer = new DefaultTracer() {
			@Override
			protected void dumpTrace(String out, Object node) {
				traces.add(out);
			}
		};
		tracer.setExchangeFormatter(formatter);
		context.setTracing(true);
		context.setTracer(tracer);
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:start").routeId("start")
					.setHeader("stage", constant("one"))
					.setVariable("var", constant("v1"))
					.split(body())
						.setHeader("item", body())
						.setHeader("done", constant(true))
					.end()
					.setHeader("stage", constant("two"));
			}
		});
		context.start();

		context.createProducerTemplate().sendBodyAndHeader("direct:start", Arrays.asList("x", "y"), "Authorization", "Bearer secret");

		String all = String.join("\n", traces);
		// Tracer runs before each node, so a trace shows the changes made by the previous node.
		assertEquals(1, traces.stream().filter(t -> t.contains(", Headers: {")).count(), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Headers: {stage=one}")), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Variables: {var=v1}")), all);
		assertEquals(2, traces.stream().filter(t -> t.contains("From: ") && t.contains("CamelSplitIndex=")).count(), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Headers: {item=x}")), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Headers: {item=y}")), all);
		assertTrue(traces.stream().anyMatch(t -> t.contains("Changed Headers: {stage=two}")), all);
		assertTrue(traces.stream().noneMatch(t -> t.contains("secret") || t.contains("TraceSnapshot")), all);
	}
}
