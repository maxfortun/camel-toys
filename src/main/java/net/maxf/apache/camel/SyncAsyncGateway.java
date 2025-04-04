package net.maxf.apache.camel;

import java.util.Map;
import java.util.HashMap;

import org.apache.camel.Exchange;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SyncAsyncGateway {
	private static final Logger logger = LogManager.getLogger(SyncAsyncGateway.class);

	private Map<String, Exchange> requests = new HashMap<>();
	private String replyToHeader = "reply-to";
	private String replyIdHeader = "reply-id";
	private String replyTo = null;
	private Long timeout = 0l;

	public void setReplyToHeader(String replyToHeader) {
		this.replyToHeader = replyToHeader;
		logger.info("replyToHeader: "+this.replyToHeader);
	}

	public String getReplyToHeader() {
		return replyToHeader;
	}

	public void setReplyIdHeader(String replyIdHeader) {
		this.replyIdHeader = replyIdHeader;
		logger.info("replyIdHeader: "+this.replyIdHeader);
	}

	public String getReplyIdHeader() {
		return replyIdHeader;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
		logger.info("replyTo: "+this.replyTo);
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
		logger.info("timeout: "+this.timeout);
	}

	public Long getTimeout() {
		return timeout;
	}

	public void queueForReply(Exchange request) {
		logger.debug("queueForReply request: "+request.getExchangeId());
		request.getIn().setHeader(replyToHeader, replyTo);
		request.getIn().setHeader(replyIdHeader, request.getExchangeId());
		synchronized(requests) {
			requests.put(request.getExchangeId(), request);
		}
	}

	public void waitForReply(Exchange request) {
		logger.debug("waitForReply request: "+request.getExchangeId());

		synchronized(request) {
			try {
				request.wait(timeout);
			} catch(InterruptedException e) {
				logger.debug("waitForReply wait("+timeout+") interrupted.", e);
			}
		}

		synchronized(requests) {
			requests.remove(request.getExchangeId());
		}

		if(null != request.getOut().getHeader(replyToHeader)) {
			logger.debug("Timed out waiting for reply. "+request.getExchangeId());
			request.getOut().removeHeader(replyToHeader);
			request.getOut().removeHeader(replyIdHeader);
			request.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, "504");
			request.getOut().setHeader(Exchange.CONTENT_TYPE, "text/plain");
			request.getOut().setBody("SyncAsync Gateway Timeout");
		}

	}

	public void notifyOfReply(Exchange response) {
		logger.debug("notifyOfReply response: "+response.getExchangeId());
		String requestId = response.getIn().getHeader(replyIdHeader, String.class);
		
		Exchange request = null;
		synchronized(requests) {
			request = requests.get(requestId);
		}

		if(null == request) {
			logger.debug("notifyOfReply request not found: "+requestId);
			return;
		}

		logger.debug("notifyOfReply request: "+requestId);
		request.getOut().setBody(response.getIn().getBody());
		request.getOut().setHeaders(response.getIn().getHeaders());
		request.getOut().removeHeader(replyToHeader);
		request.getOut().removeHeader(replyIdHeader);
		synchronized(request) {
			request.notify();
		}
	}
}

