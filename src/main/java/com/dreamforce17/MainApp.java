package com.dreamforce17;

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.camel.salesforce.dto.Case;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MainApp {
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
    	
    	// check if we have predefined port, otherwise use default
    	String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            webPort = "8084";
        }
        
        // create http server
        Server server = new Server(Integer.valueOf(webPort));
        
        // setup camel servlet
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/camel");
        context.addServlet(new ServletHolder("CamelServlet", new CamelHttpTransportServlet()),"/*");
        server.setHandler(context);
        
        // create camel context
        CamelContext camelContext = new DefaultCamelContext();
        
        // add route
        camelContext.addRoutes(new RouteBuilder() {
			public void configure() {

				from("servlet:///cases")
				
					// log incoming body
					.log("Receive body = ${body}")
				
					// remember current exchange we will need to to send response back
					.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							ExchangeManager.saveExchange(exchange);
						}
					})
					
					// http is streaming protocol, enable streaming to simplify next splitting
					.streamCaching()
					
					// convert body from JSON text to List of Salesforce cases
					.unmarshal(new ListJacksonDataFormat(Case.class))
					
					// split the List of case to single Cases (there might be many of them)
					.split().body()
					
					// prepare info to sent back to Telegram chat
					.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							// save original exchange id (to find it out later for sending response back to Salesforce)
							exchange.setProperty("OriginalExchangeId", exchange.getProperty("CamelCorrelationId", String.class));
							// obtain Salesforce case (it's in the body)
							Case c = exchange.getIn().getBody(Case.class);
							// save Case to create response back to Salesforce
							exchange.setProperty("CaseId", c.getId());
							// create Telegram message
							OutgoingTextMessage messageOut = new OutgoingTextMessage();
							messageOut.setText("Case " + c.getId() + " is now " + c.getStatus().toString());
							messageOut.setChatId(c.getTelegram_Chat_Id__c());
							// save Telegram message to sent out
							exchange.getIn().setBody(messageOut);
						}
					})
					
					// send Telegram message (it was prepared on the prev state)
					.to("telegram:bots/359951231:AAE3dmv5LB3tnUzeRpzJI8c7srBZi9wG55s")
					
					// now we need to aggregate all Telegram responses to create one response back to Salesforce
					/*
					 * constant(true) means we need to collect all messages in ones
					 * GroupedExchangeAggregationStrategy means we need to create simple list of Exchanges (each Exchange is result of Telegram message sent)
					 * */
					.aggregate(constant(true), new GroupedExchangeAggregationStrategy()).completion(new Predicate() {
						/*
						 * The predicate is supposed to find out when we need to stop aggregate
						 * We need to stop as soon as current aggregated item number equals total splitted number
						 * */
						@Override
						public boolean matches(Exchange exchange) {
							@SuppressWarnings("unchecked")
							List<Exchange> groupedExchange = (List<Exchange>)exchange.getProperty(Exchange.GROUPED_EXCHANGE);
							// total number of splited messages
							int total = groupedExchange.get(0).getProperty(Exchange.SPLIT_SIZE, Integer.class);
							// current aggregated message number
							int current = exchange.getProperty(Exchange.AGGREGATED_SIZE, Integer.class);
							return total == current;
						}
					})
					
					// final step - we need to create HTTP response back to Salesforce
					.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							@SuppressWarnings("unchecked")
							// obtain list of all the exchanges (each is result of sending Telegram message)
							List<Exchange> groupedExchange = (List<Exchange>)exchange.getIn().getBody();
							StringBuilder response = new StringBuilder();
							// create HTTP JSON response (this is a map CaseId to status (success or failure))
							response.append("[");
							for(int i = 0; i < groupedExchange.size(); i++) {
								Exchange e = groupedExchange.get(i);
								response.append("{\"");
								response.append(e.getProperty("CaseId", String.class));
								response.append("\":\"");
								response.append( (e.getException() == null) ? "success" : "failure");
								response.append("\"}");
								if (i != groupedExchange.size() - 1) response.append(",");
							}
							response.append("]");
							// find out original Exchange - related to initial HTTP request and save HTTP reponse there
							ExchangeManager.retrieveExchange(groupedExchange.get(0).getProperty("OriginalExchangeId", String.class)).getOut().setBody(response);
						}
					})
				;		
			}
        });
        
        // start Camel and webserver
        camelContext.start();
        server.start();
        
        // wait until web server is terminated and stop camel
        server.join();
        camelContext.stop();
    }

}
