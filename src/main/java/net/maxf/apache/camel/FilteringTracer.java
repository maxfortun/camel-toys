package net.maxf.apache.camel;

import org.apache.camel.impl.engine.DefaultTracer;
import org.apache.camel.spi.ExchangeFormatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DefaultTracer that does not log traces its FilteringExchangeFormatter marked as skipped,
 * e.g. deltaMode traces with no changes when traceUnchanged is off.
 */
public class FilteringTracer extends DefaultTracer {
	private static final Logger logger = LogManager.getLogger(FilteringTracer.class);

	@Override
	protected void dumpTrace(String out, Object node) {
		ExchangeFormatter formatter = getExchangeFormatter();
		if(formatter instanceof FilteringExchangeFormatter && ((FilteringExchangeFormatter)formatter).isTraceSkipped()) {
			logger.trace("Skipped trace {}.", out);
			return;
		}
		writeTrace(out, node);
	}

	/** Logs a trace that was not skipped. */
	protected void writeTrace(String out, Object node) {
		super.dumpTrace(out, node);
	}
}
