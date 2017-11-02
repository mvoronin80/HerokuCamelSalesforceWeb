package com.dreamforce17;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.Exchange;

public class ExchangeManager extends MainApp {
	
	static Map<String, Exchange> map = new ConcurrentHashMap<String, Exchange>();
	
	static void saveExchange(Exchange exchange) {
		map.put(exchange.getExchangeId(), exchange);
	}
	
	static Exchange retrieveExchange(String exchangeId) {
		Exchange result = map.get(exchangeId);
		map.remove(exchangeId);
		return result;
	}
	
	static boolean hasExchange(String exchangeId) {
		return map.containsKey(exchangeId);
	}
}
