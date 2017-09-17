package com.dreamforce17;

import org.eclipse.jetty.server.Server;

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
        Server server = new Server();
        ServletContextHandler context=new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/camel");
        
        ServletHolder holderPwd = new ServletHolder("HelloServlet", new HelloServlet());
        context.addServlet(holderPwd, "/*");
        server.setHandler(context);
        
        server.start();
        server.join();   
    }

}
