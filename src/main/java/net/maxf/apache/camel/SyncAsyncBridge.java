package net.maxf.apache.camel;

import java.util.Map;
import java.util.HashMap;

import org.apache.camel.Exchange;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SyncAsyncBridge {
	private static final Logger logger = LogManager.getLogger(SyncAsyncBridge.class);

	private Map<String, Exchange> requests = new HashMap<>();
	private String replyTo = null;

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void queueForReply(Exchange request) {
		logger.debug("Request: "+request.getExchangeId());
		request.getIn().setHeader("reply-to", replyTo);
		request.getIn().setHeader("reply-id", request.getExchangeId());
		synchronized(request) {
			requests.put(request.getExchangeId(), request);
		}
	}

	public void waitForReply(Exchange request, long timeout) throws InterruptedException {
		logger.debug("Request: "+request.getExchangeId());
		synchronized(request) {
			request.wait(timeout);
		}
	}

	public void notifyOfReply(Exchange response) {
		logger.debug("Response: "+response.getExchangeId());
		String requestId = response.getIn().getHeader("reply-id", String.class);
		
		Exchange request = requests.get(requestId);
		if(null == request) {
			logger.debug("Request not found: "+requestId);
			return;
		}

		logger.debug("Request: "+requestId);
		request.getOut().setBody(response.getIn().getBody());
        request.getOut().setHeaders(response.getIn().getHeaders());
	}
}

