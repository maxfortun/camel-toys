#!/usr/bin/env node

const autocannon = require('autocannon')

const url = 'http://localhost:3101/sync';
let reqId = 0;

const instance = autocannon({
	url,
	connections: 100,
	pipelining: 1,
	timeout: 60,
	duration: 60,
	requests: [{
		method: 'POST',
		setupRequest: (req, context) => {
			req.path = `${req.path}?req-id=${reqId++}`;
    		return req;
		},
		onResponse: (status, body, context, headers) => {
			console.log('onResponse:', status, body);
		}
	}]
}, console.log);


