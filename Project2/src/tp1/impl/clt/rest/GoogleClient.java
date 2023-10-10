package tp1.impl.clt.rest;

import javax.net.ssl.HttpsURLConnection;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.impl.dropbox.args.GoogleResponse;
import tp1.impl.utils.InsecureHostnameVerifier;

public class GoogleClient {
	
	protected static final int READ_TIMEOUT = 5000;
	protected static final int CONNECT_TIMEOUT = 5000;
	protected static final int RETRY_SLEEP = 1000;
	protected static final int MAX_RETRIES = 10;
	public final static long RETRY_PERIOD = 1000;
	
	protected final Client client;
	protected final WebTarget target;
	protected final ClientConfig config;
	
	
	
	private static final String uri = "https://sheets.googleapis.com/v4/spreadsheets";
	
	public GoogleClient () {
		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
		
		this.config = new ClientConfig();
		this.config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		this.config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
		this.config.property(ClientProperties.FOLLOW_REDIRECTS, true);
		
		this.client = ClientBuilder.newClient(config);
		this.target = this.client.target(uri);	
	}
	
	public String[][] getValues (String sheetId, String range, String key) {
		
		short retries = 0;
		boolean success = false;
		GoogleResponse u = null;
		String [][] values = null;
		while(!success && retries < MAX_RETRIES) {
			try {
			Response r = target.path(sheetId).path("values").path(range).queryParam("key", key).request()
					.accept(MediaType.APPLICATION_JSON)
					.get();
			System.out.println(target);
			System.out.println(r);

			if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
				System.out.println("Success:");
				u = r.readEntity (GoogleResponse.class);
				values = u.v();
			} else {
				System.out.println("Error, HTTP error status: " + r.getStatus() );
			}
			success = true;
			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				pe.printStackTrace();
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
		return values;
	}

}
