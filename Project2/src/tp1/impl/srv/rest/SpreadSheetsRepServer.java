package tp1.impl.srv.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.srv.Domain;

public class SpreadSheetsRepServer extends AbstractRestServer{
	
	public static final int PORT = 8800;
	public static final String SERVICE_NAME = "sheets";
	
	private static Logger Log = Logger.getLogger(SpreadSheetsRepServer.class.getName());
	public static String secret;
	public static String serverType;
	
	SpreadSheetsRepServer(int port) {
		super(Log, SERVICE_NAME, port);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.register( new SpreadSheetsRepResources() );
		config.register( GenericExceptionMapper.class );
		config.register( CustomLoggingFilter.class);
	}
	
	public static void main(String[] args) throws Exception {
		Domain.set(args.length > 0 ? args[0] : "?");	
		//int port = args.length < 2 ? PORT : Integer.valueOf(args[1]);
		secret = args[1];
		serverType = args[2];
		

		Log.setLevel( Level.ALL );
		
		new SpreadSheetsRepServer(PORT).start();
		
	}	
}
