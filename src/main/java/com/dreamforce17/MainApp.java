package com.dreamforce17;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MainApp {
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
    	String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            webPort = "8082";
        }
        Server server = new Server(Integer.valueOf(webPort));
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/camel");
        context.addServlet(new ServletHolder("CamelServlet", new CamelHttpTransportServlet()),"/*");
        server.setHandler(context);
        
        CamelContext camelContext = new DefaultCamelContext();
        
        camelContext.addRoutes(new RouteBuilder() {
			public void configure() {
				from("servlet:///cases").
					log("Received body ${body}");
				;
			}
        });
        
        camelContext.start();
        server.start();
        server.join();  
        camelContext.stop();
    }

}
