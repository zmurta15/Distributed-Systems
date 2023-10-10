package tp1.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.discovery.Discovery;
import tp1.resources.SpreadsheetsResources;


public class SpreadsheetsServer {
	
	private static Logger Log = Logger.getLogger(SpreadsheetsServer.class.getName());
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	public static final String SERVICE = "sheets";
	
	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			
			ResourceConfig config = new ResourceConfig();
			config.register(new SpreadsheetsResources(args[0]));

			String serverURI = String.format("http://%s:%s/rest", ip, PORT);
			JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
		
			Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
			
			Discovery.getInstance().start(DISCOVERY_ADDR, args[0]+":"+SERVICE, serverURI);
			
		}
		catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
}
