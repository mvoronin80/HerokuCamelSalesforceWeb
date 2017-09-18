package com.dreamforce17;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * 
 * This class launches the web application in an embedded Jetty container.
 * This is the entry point to your application. The Java command that is used for
 * launching should fire this main method.
 *
 */
public class Main {
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
    	String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        Server server = new Server(Integer.valueOf(webPort));
        ServletContextHandler servletContext = new ServletContextHandler();
        servletContext.setContextPath("/camel");
        
        ServletHolder holderPwd = new ServletHolder("HelloServlet", new HelloServlet());
        servletContext.addServlet(holderPwd, "/*");
        server.setHandler(servletContext);
        
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
