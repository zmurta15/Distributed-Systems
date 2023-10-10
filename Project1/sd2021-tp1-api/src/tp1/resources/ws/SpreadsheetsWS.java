package tp1.resources.ws;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;


import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.engine.SpreadsheetUseSoap;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;


@WebService(serviceName=SoapSpreadsheets.NAME, targetNamespace=SoapSpreadsheets.NAMESPACE, endpointInterface=SoapSpreadsheets.INTERFACE)
public class SpreadsheetsWS implements SoapSpreadsheets{
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 15000;
	public final static int REPLY_TIMEOUT = 600;
	public final static String USERS_WSDL = "/users/?wsdl";
	
	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private static Logger Log = Logger.getLogger(SpreadsheetsWS.class.getName());
	private int id;
	private String domain;
	private SpreadsheetEngine spe = SpreadsheetEngineImpl.getInstance();
	private final Map<String, String[][]> inCache = new HashMap<String, String[][]>(); 
	
	public SpreadsheetsWS(String domain) {
		this.id = 0;
		this.domain = domain;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
	Log.info("createSpreadsheet : "+sheet.getOwner());
		
		User u = getUser(sheet.getOwner(), domain);

		if(u == null ) {
			Log.info("User does not exist.");
			throw new SheetsException("User does not exist");
		}
		
		if(!u.getPassword().equals(password)) {
			Log.info("Wrong password.");
			throw new SheetsException("Wrong password");
		}
		
		if(sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("Dimension not allowed.");
			throw new SheetsException("Dimension not allowed");
		}
		
		sheet.setSheetId(sheet.getOwner() + id);
		id++;
		sheet.setSheetURL("http://sheets."+domain+":8080/soap/spreadsheets/" + sheet.getSheetId());
		synchronized (this) {
			sheets.put(sheet.getSheetId(), sheet);
		}
		
		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		Spreadsheet s = null;

		Log.info("Deleting spreadsheet with the id: "+sheetId);
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null) {
			Log.info("Sheet does not exist");
			throw new SheetsException("Sheet does not exist");
		}
		
		User u = getUser(s.getOwner(), domain);
		
		if(!u.getPassword().equals(password)) {
			Log.info("Incorrect password.");
			throw new SheetsException("Incorrect password"); 
		}
		
		synchronized(this) {
			sheets.remove(sheetId);
		}
		inCache.remove(sheetId);
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Spreadsheet s = null;

		Log.info("getSpreadsheet : sheetId " +sheetId);
		
		User u = getUser(userId, domain);
		
		synchronized (this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null) {
			Log.info("Sheet or user do not exist");
			throw new SheetsException("Sheet or user do not exist");
		}
		
		if(!u.getPassword().equals(password)) {
			Log.info("Password is incorrect");
			throw new SheetsException("Password is incorrect");
		}
		
		if(s.getSharedWith() == null) {
			s.setSharedWith(new HashSet<String>());
		}
		if(!s.getSharedWith().contains(userId + "@"+domain) && !u.getUserId().equals(s.getOwner())) {
			Log.info("Sheet not shared");
			throw new SheetsException("Sheet not shared");
		}
		
		return s;
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("Sharing spreadshit "+sheetId);
		
		String[] temp = userId.split("@");

		User u = getUser(temp[0], temp[1]);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null) {
			Log.info("Spreadsheet or user do not exist");
			throw new SheetsException("Spreadsheet or user do not exist");
		}
		
		if(s.getSharedWith().contains(userId)) {
			Log.info("This sheet is already shared with "+userId);
			throw new SheetsException("This sheet is already shared with "+userId);
		}
		
		User owner = getUser(s.getOwner(), domain);
		if(!owner.getPassword().equals(password)) {
			Log.info("The password is incorrect");
			throw new SheetsException("The password is incorrect");
		}
		Set<String> aux = s.getSharedWith();
		aux.add(userId);
		s.setSharedWith(aux);
		synchronized(this) {
			sheets.put(sheetId, s);
		}
		
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("Unsharing spreadsheet "+sheetId);
		
		String[] temp = userId.split("@");

		User u = getUser(temp[0], temp[1]);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		if(s == null || u == null || !s.getSharedWith().contains(userId)) {
			Log.info("Sheet or user do not exist or the sheet is not shared");
			throw new SheetsException("Sheet or user do not exist or the sheet is not shared");
		}
		User owner = getUser(s.getOwner(),domain);
		if(!owner.getPassword().equals(password)) {
			Log.info("The password is incorrect");
			throw new SheetsException("The password is incorrect");
		}
		
		Set<String> aux = s.getSharedWith();
		aux.remove(userId);
		s.setSharedWith(aux);
	
		synchronized(this) {
			sheets.put(sheetId, s);
		}
		
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException {
		Log.info("Updating cell value "+ cell + " in the sheet "+sheetId);
		
		User u = getUser(userId, domain);
		Spreadsheet s = null;
		
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		
		if(s == null) {
			Log.info("Sheet does not exist");
			throw new SheetsException("Sheet does not exist");
		}
		
		if(!u.getPassword().equals(password)) {
			Log.info("Password is incorrect");
			throw new SheetsException("Password is incorrect");
		}
		
		s.setCellRawValue(cell, rawValue);
		
		synchronized(this) {
			sheets.put(sheetId, s);
		}
		inCache.remove(sheetId); 
		
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		String[][] result = null;
	
		Spreadsheet s = null;
		synchronized(this) {
			s = sheets.get(sheetId);
		}
	
		User u = getUser(userId, domain);
	
		if(s == null || u == null) {
			Log.info("Sheet or user do not exist");
			throw new SheetsException("Sheet or user do not exist");
		}
	
		if(!s.getSharedWith().contains(userId + "@"+domain) && !u.getUserId().equals(s.getOwner()) || !u.getPassword().equals(password)) {
			Log.info("Sheet not shared or not the owner or incorrect password");
			throw new SheetsException("Sheet not shared or not the owner or incorrect password");
		}
		
		if(inCache.containsKey(sheetId)) {
			result = inCache.get(sheetId);
		}
		else {
			result = spe.computeSpreadsheetValues(new SpreadsheetUseSoap(s));
			inCache.put(sheetId, result);
		}
		
		return result;
	}
	
	
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
	public String[][] getSpreadImport(String sheetId) throws SheetsException {
		Spreadsheet s = null;
			
		synchronized(this) {
			s = sheets.get(sheetId);
		}
		
		return spe.computeSpreadsheetValues(new SpreadsheetUseSoap(s));
	}
	
	/**
	 * Connects the user's server to get the user with the given id and domain
	 * @param userId - the id of the user
	 * @param domainUser - the domain of the user
	 * @return the user with the given id and domain
	 */
	private User getUser(String userId, String domainUser){
		List<String> ur = Discovery.getInstance().knownUrisOf(domainUser+":users");
		SoapUsers users = null;
		
		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create( new URL(ur.get(0) + USERS_WSDL), QNAME );
			users = service.getPort( tp1.api.service.soap.SoapUsers.class );
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Set timeouts for executing operations
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
	
		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;
		User u = null;

		while(!success && retries < MAX_RETRIES) {
			try {
				u = users.getUserAux(userId);
				success = true;
			}
			catch (UsersException e) {
				System.out.println("Cound not get user: " + e.getMessage());
				success = true;
			}
			catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
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
