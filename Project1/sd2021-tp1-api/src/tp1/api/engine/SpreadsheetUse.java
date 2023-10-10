package tp1.api.engine;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.util.CellRange;

public class SpreadsheetUse extends SpreadsheetUseSoap implements AbstractSpreadsheet{
	
	private Client client;

	
	public SpreadsheetUse(Spreadsheet s) {
		super(s);
		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		client = ClientBuilder.newClient(config);
	}
	

	@Override
	public String[][] getRangeValues(String sheetURL, String range) {
	
		WebTarget target = client.target(sheetURL);
		
		short retries = 0;
		boolean success = false;
		
		String[][] result = null;
		
		while(!success && retries < MAX_RETRIES) {
			
			try {
			Response r = target.path("sheetImport").request()
					.accept(MediaType.APPLICATION_JSON)
					.get();
			

			if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
				System.out.println("Success:");
				result = r.readEntity(String[][].class);
			} else
				System.out.println("Error, HTTP error status: " + r.getStatus() );
			
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
		
		CellRange cell = new CellRange(range);
		
		return cell.extractRangeValuesFrom(result);
	}

}
