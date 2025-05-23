package net.maxf.apache.camel;

import java.util.Map;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.spi.ExchangeFormatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SyncAsyncGateway {
	private static final Logger logger = LogManager.getLogger(SyncAsyncGateway.class);

	private ExchangeFormatter exchangeFormatter = null;

	private Map<String, Exchange> requests = new HashMap<>();
	private String requestIdHeader = "req-id";
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

	public void setExchangeFormatter(ExchangeFormatter exchangeFormatter) {
		this.exchangeFormatter = exchangeFormatter;
	}

	public ExchangeFormatter getExchangeFormatter(Exchange exchange) {
		if(null != exchangeFormatter) {
			return exchangeFormatter;
		}

		synchronized(this) {
			if(null != exchangeFormatter) {
				return exchangeFormatter;
			}

			exchangeFormatter = exchange.getContext().getRegistry().lookupByNameAndType("logFormatter", ExchangeFormatter.class);
		}

		return exchangeFormatter;
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
		synchronized(request) {
			logger.debug("waitForReply request: "+request.getExchangeId());
			try {
				request.wait(timeout);
			} catch(InterruptedException e) {
				logger.debug("waitForReply wait("+timeout+") interrupted: "+request.getExchangeId(), e);
			}
		}

		logger.debug("waitForReply request after wait: "+request.getExchangeId());
		logger.trace("waitForReply request after wait: "+request.getExchangeId()+" "+getExchangeFormatter(request).format(request));

		synchronized(requests) {
			requests.remove(request.getExchangeId());
		}

		if(null != request.getIn().getHeader(replyToHeader)) {
			logger.debug("waitForReply timed out waiting for reply: "+request.getExchangeId());

			String message = "SyncAsync Gateway Timeout";
			String requestId = request.getIn().getHeader(requestIdHeader, String.class);
			if(null != requestId) {
				message+=" ("+requestId+")";
			}
 
			request.getIn().removeHeader(replyToHeader);
			request.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, "504");
			request.getIn().setHeader(Exchange.HTTP_RESPONSE_TEXT, message);
			request.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
			request.getIn().setBody(message);
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

		synchronized(request) {
			logger.debug("notifyOfReply request: "+requestId);
			request.getIn().setBody(response.getIn().getBody());
			request.getIn().setHeaders(response.getIn().getHeaders());
			request.getIn().removeHeader(replyToHeader);
			request.getIn().removeHeader(replyToHeader);
			logger.trace("notifyOfReply request: "+getExchangeFormatter(request).format(request));
			request.notifyAll();
		}
	}
}

