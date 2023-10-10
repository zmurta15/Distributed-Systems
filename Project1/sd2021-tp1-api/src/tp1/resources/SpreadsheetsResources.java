package tp1.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.*;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.engine.SpreadsheetUse;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import java.util.*;
import java.util.logging.Logger;


import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;




@Singleton
public class SpreadsheetsResources implements RestSpreadsheets{
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 15000;
	public final static int REPLY_TIMEOUT = 600;
	
	private int id;

	
	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private static Logger Log = Logger.getLogger(SpreadsheetsResources.class.getName());
	private SpreadsheetEngine spe = SpreadsheetEngineImpl.getInstance();
	private String domain;
	private final Map<String, String[][]> inCache = new HashMap<String, String[][]>();
	private Client client;
	
	public SpreadsheetsResources() {
		// TODO Auto-generated constructor stub
	}
	
	public SpreadsheetsResources(String domain) {
		this.id = 0;
		this.domain = domain;
		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		client = ClientBuilder.newClient(config);
	}
	
	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("createSpreadsheet : "+sheet.getOwner());
		
		User u = getUser(sheet.getOwner(), domain);

		if(u == null ) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		Log.info("User: " +u.toString());
		
		if(!u.getPassword().equals(password)) {
			Log.info("Wrong password.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		if(sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("Dimension not allowed.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		sheet.setSheetId(sheet.getOwner() + id);
		id++;
		sheet.setSheetURL("http://sheets."+domain+":8080/rest/spreadsheets/" + sheet.getSheetId());
		synchronized (this) {
			sheets.put(sheet.getSheetId(), sheet);
		}
		
		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		Spreadsheet s = null;
		
		Log.info("Deleting spreadsheet with the id: "+sheetId);
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null) {
			Log.info("Sheet does not exist");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		User u = getUser(s.getOwner(), domain);
		
		if(!u.getPassword().equals(password)) {
			Log.info("Incorrect password.");
			throw new WebApplicationException(Status.FORBIDDEN); 
		}
		
		synchronized(this) {
			sheets.remove(sheetId);
		}
		inCache.remove(sheetId);
		
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		Spreadsheet s = null;

		Log.info("getSpreadsheet : sheetId " +sheetId);
		
		User u = getUser(userId, domain);
		
		synchronized (this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null) {
			Log.info("Sheet or user do not exist");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		if(!u.getPassword().equals(password)) {
			Log.info("Password is incorrect");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		if(!s.getSharedWith().contains(userId + "@"+domain) && !u.getUserId().equals(s.getOwner())) {
			Log.info("Sheet not shared");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		return s;
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		String[][] result = null;
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		User u = getUser(userId, domain);
		
		if(s == null || u == null) {
			Log.info("Sheet or user do not exist");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		if(!s.getSharedWith().contains(userId + "@"+domain) && !u.getUserId().equals(s.getOwner()) || !u.getPassword().equals(password)) {
			Log.info("Sheet not shared or not the owner or incorrect password");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		if(inCache.containsKey(sheetId)) {
			result = inCache.get(sheetId);
		}
		else {
			result = spe.computeSpreadsheetValues(new SpreadsheetUse(s));
			inCache.put(sheetId, result);
		}
		
		return result;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		Log.info("Updating cell value "+ cell + " in the sheet "+sheetId);
		
		User u = getUser(userId, domain);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null) {
			Log.info("Sheet does not exist");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		if(!u.getPassword().equals(password)) {
			Log.info("Password is incorrect");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		s.setCellRawValue(cell, rawValue);
		
		synchronized(this) {
			sheets.put(sheetId, s);
		}
		inCache.remove(sheetId);
		
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		Log.info("Sharing spreadshit "+sheetId);
		
		String[] temp = userId.split("@");

		User u = getUser(temp[0], temp[1]);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null) {
			Log.info("Spreadsheet or user do not exist");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		if(s.getSharedWith().contains(userId)) {
			Log.info("This sheet is already shared with "+userId);
			throw new WebApplicationException(Status.CONFLICT);
		}
		
		User owner = getUser(s.getOwner(), domain);
		if(!owner.getPassword().equals(password)) {
			Log.info("The password is incorrect");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		Set<String> aux = s.getSharedWith();
		aux.add(userId);
		s.setSharedWith(aux);
		
		synchronized(this) {
			sheets.put(sheetId, s);
		}
		
		
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		Log.info("Unsharing spreadsheet "+sheetId);
		
		String[] temp = userId.split("@");

		User u = getUser(temp[0], temp[1]);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null || !s.getSharedWith().contains(userId)) {
			Log.info("Sheet or user do not exist or the sheet is not shared");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		User owner = getUser(s.getOwner(),domain);
		if(!owner.getPassword().equals(password)) {
			Log.info("The password is incorrect");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		Set<String> aux = s.getSharedWith();
		aux.remove(userId);
		s.setSharedWith(aux);
	
		synchronized(this) {
			sheets.put(sheetId, s);
		}
	}
	
	@Override
	public void deleteSheetsFromUser(String userId) {
		synchronized(this) {
			Collection<Spreadsheet> c = sheets.values();
			for(Spreadsheet s: c) {
				if(s.getOwner().equals(userId)) {
					sheets.remove(s.getSheetId());
					inCache.remove(s.getSheetId()); 
				}
			}
		}
	}

	@Override
	public String[][] getSpreadImport(String sheetId) {
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		return spe.computeSpreadsheetValues(new SpreadsheetUse(s));
	}
	
	/**
	 * Connects the user's server to get the user with the given id and domain
	 * @param userId - the id of the user
	 * @param domainUser - the domain of the user
	 * @return the user with the given id and domain
	 */
	private User getUser(String userId, String domainUser) {
		
		List<String> ur = Discovery.getInstance().knownUrisOf(domainUser+":users");
		WebTarget target = client.target(ur.get(0)).path( RestUsers.PATH );
		short retries = 0;
		boolean success = false;
		User u = null;

		while(!success && retries < MAX_RETRIES) {
			
			try {
			Response r = target.path(userId).path("aux").request()
					.accept(MediaType.APPLICATION_JSON)
					.get();
			

			if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
				System.out.println("Success:");
				u = r.readEntity(User.class);
				System.out.println( "User : " + u);
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
		return u;
	}
	
	
	
}
