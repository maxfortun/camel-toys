package net.maxf.apache.camel;

import java.util.Map;
import java.util.HashMap;

import org.apache.camel.Exchange;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SyncAsyncBridge {
	private static final Logger logger = LogManager.getLogger(SyncAsyncBridge.class);

	private Map<String, Exchange> requests = new HashMap<>();

	public void queueForReply(Exchange request) {
		logger.debug(request.getExchangeId());
		synchronized(request) {
			requests.put(request.getExchangeId(), request);
		}
	}

	public void waitForReply(Exchange request, long timeout) throws InterruptedException {
		logger.debug(request.getExchangeId());
		synchronized(request) {
			request.wait(timeout);
		}
	}

	public void notifyOfReply(Exchange response) {
		logger.debug(response.getExchangeId());
		// Exchange request = null;
	}
}

