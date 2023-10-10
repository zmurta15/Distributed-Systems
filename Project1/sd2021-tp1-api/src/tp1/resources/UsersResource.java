package tp1.resources;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.discovery.Discovery;



@Singleton
public class UsersResource implements RestUsers {
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 15000;
	public final static int REPLY_TIMEOUT = 600;

	private final Map<String, User> users = new HashMap<String, User>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());
	private String domain;
	private Client client;

	public UsersResource() {
	}
	
	public UsersResource(String domain) {
		this.domain = domain;
		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		client = ClientBuilder.newClient(config);
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {

			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}

			// Add the user to the map of users
			users.put(user.getUserId(), user);

		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);
		
		User user = null;

		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);

		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User user1 = null;

		synchronized (this) {
			user1 = users.get(userId);
		}

		// Check if user exists
		if (user1 == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user1.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		if(user.getEmail() != null) {
			user1.setEmail(user.getEmail());
		}
		if(user.getPassword() != null) {
			user1.setPassword(user.getPassword());
		}
		if(user.getFullName() != null) {
			user1.setFullName(user.getFullName());
		}
		
		synchronized (this) {
			users.replace(userId, user1);
		}
		
		return user1;
	}

	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		// Check if user is valid, if not return HTTP CONFLICT (409)
		
		User user = null;
		synchronized (this) {
			user = users.get(userId);

		}
		
		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		synchronized (this) {
			users.remove(userId);
		}
		deleteSpreads(userId);
		
		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);

		List<User> auxList = new ArrayList<User>();
		Collection<User> us = null;
		synchronized (this) {
			 us = users.values();
			 for(User u : us) {
				 if(u.getFullName().toLowerCase().contains(pattern.toLowerCase())) {
					 auxList.add(u);
				 }
			}
		}
		
		return auxList;
	}

	@Override
	public User getUserAux(String userId) {
		User user = null;

		synchronized (this) {
			user = users.get(userId);
		}
		return user;
	}
	
	/**
	 * Connects with the spreadsheets server to delete all the spreadsheets of the user with the given Id when the user is deleted
	 * @param userId - the id of the user 
	 */
	private void deleteSpreads(String userId) {
		List<String> ur = Discovery.getInstance().knownUrisOf(domain+":sheets");
		WebTarget target = client.target(ur.get(0)).path( RestSpreadsheets.PATH );
		short retries = 0;
		boolean success = false;
		Spreadsheet s = null;
		
		while(!success && retries < MAX_RETRIES) {
			try {
			Response r = target.path(userId).path("delsheet").request()
					.accept(MediaType.APPLICATION_JSON)
					.delete();
			

			if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
				System.out.println("Success:");
				s = r.readEntity(Spreadsheet.class);
				System.out.println( "Spreadsheet : " + s);
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
	}
}
