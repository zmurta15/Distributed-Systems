package tp1.resources.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.soap.*;
import tp1.discovery.Discovery;
import tp1.api.User;

@WebService(serviceName=SoapUsers.NAME, 
targetNamespace=SoapUsers.NAMESPACE, 
endpointInterface=SoapUsers.INTERFACE)
public class UsersWS implements SoapUsers {

	private final Map<String,User> users;

	private static Logger Log = Logger.getLogger(UsersWS.class.getName());
	private String domain;
	
	public final static String SHEETS_WSDL = "/spreadsheets/?wsdl";
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 15000;
	public final static int REPLY_TIMEOUT = 600;

	public UsersWS(String domain) {
		this.users = new HashMap<String, User>();
		this.domain = domain;
	}

	@Override
	public String createUser(User user) throws UsersException {
		Log.info("createUser : " + user);

		// Check if user is valid, if not throw exception
		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new UsersException("Invalid user instance.");
		}

		synchronized (this) {
			// Check if userId does not exist exists, if not throw exception
			if( users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new UsersException("User already exists.");
			}

			//Add the user to the map of users
			users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) throws UsersException {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid, if not throw exception
		if(userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or password are null.");
		}

		User user = null;
		
		synchronized (this) {
			user = users.get(userId);
		}
		 

		// Check if user exists, if yes throw exception
		if( user == null ) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist.");
		}

		//Check if the password is correct, if not throw exception
		if( !user.getPassword().equals( password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect.");
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or password null");
		}

		User user1 = null;

		synchronized (this) {
			user1 = users.get(userId);
		}

		// Check if user exists
		if (user1 == null) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist");
		}

		// Check if the password is correct
		if (!user1.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect");
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
	public User deleteUser(String userId, String password) throws UsersException {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);

		User user = null;
		synchronized (this) {
			user = users.get(userId);

		}
		
		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist");
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect");
		}
		
		synchronized (this) {
			users.remove(userId);
		}
		deleteSpreads(userId);
		
		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
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
	
	public User getUserAux(String userId) throws UsersException{
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
		SoapSpreadsheets sheets = null;
		
		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create( new URL(ur.get(0) + SHEETS_WSDL), QNAME );
			sheets = service.getPort( tp1.api.service.soap.SoapSpreadsheets.class);
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT,CONNECTION_TIMEOUT);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while (!success && retries < MAX_RETRIES) {
			try {
				sheets.deleteSheetsFromUser(userId);
				success = true;
			} catch (SheetsException e) {
				System.out.println("Cound not get sheet: " + e.getMessage());
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
	}

}
