package tp1.impl.srv.rest;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.engine.AbstractSpreadsheet;
import tp1.engine.CellRange;
import tp1.engine.SpreadsheetEngine;
import tp1.impl.clt.SpreadsheetsClientFactory;
import tp1.impl.clt.UsersClientFactory;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.impl.srv.Domain;

import tp1.impl.utils.IP;
import tp1.impl.dropbox.args.*;
import tp1.impl.dropbox.replies.ListFolderReturn;
import tp1.impl.dropbox.replies.ListFolderReturn.FolderEntry;

@Singleton
public class SpreadSheetsDropboxResources implements RestSpreadsheets {
	private static Logger Log = Logger.getLogger(SpreadSheetsDropboxResources.class.getName());

	private static final long USER_CACHE_CAPACITY = 100;
	private static final long USER_CACHE_EXPIRATION = 120;
	private static final long VALUES_CACHE_CAPACITY = 100;
	private static final long VALUES_CACHE_EXPIRATION = 120;
	private static final Pattern SPREADSHEETS_URI_PATTERN = Pattern.compile("(.+)/spreadsheets/(.+)");
	
	private static String secret;

	private static final String UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload";
	private static final String DOWNLOAD_URL = "https://content.dropboxapi.com/2/files/download";
	private static final String DELETE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
	private static final String LIST_FOLDER_URL = "https://api.dropboxapi.com/2/files/list_folder";
	private static final String LIST_FOLDER_CONTINUE_URL = "https://api.dropboxapi.com/2/files/list_folder/continue";

	protected static final String JSON_CONTENT_TYPE_OCTET = "application/octet-stream";
	protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	
	
	/**
	 * Nao utilizamos dummy_set 
	 * private static final Set<String> DUMMY_SET = new HashSet<>();
	 */

	final SpreadsheetEngine engine = SpreadsheetEngineImpl.getInstance();
	final AtomicInteger counter = new AtomicInteger();

	final Map<String, Set<String>> userSheets = new ConcurrentHashMap<>();

	final String DOMAIN = '@' + Domain.get();

	LoadingCache<String, User> users = CacheBuilder.newBuilder().maximumSize(USER_CACHE_CAPACITY)
			.expireAfterWrite(USER_CACHE_EXPIRATION, TimeUnit.SECONDS).build(new CacheLoader<>() {
				@Override
				public User load(String userId) throws Exception {
					return UsersClientFactory.get().fetchUser(userId).value();
				}
			});
	/*
	 * This cache stores spreadsheet values. For local domain spreadsheets, the key
	 * is the sheetId, for remote domain spreadsheets, the key is the sheetUrl.
	 */
	Cache<String, String[][]> sheetValuesCache = CacheBuilder.newBuilder().maximumSize(VALUES_CACHE_CAPACITY)
			.expireAfterWrite(VALUES_CACHE_EXPIRATION, TimeUnit.SECONDS).build();

	private String APIKey = SpreadSheetsDropboxServer.APIKey;
	private String APISecret = SpreadSheetsDropboxServer.APISecret;
	private String aToken = SpreadSheetsDropboxServer.aToken;
	private OAuth20Service service;
	private OAuth2AccessToken accessToken;
	private Gson json;

	
	public SpreadSheetsDropboxResources() {
		service = new ServiceBuilder(APIKey).apiSecret(APISecret).build(DropboxApi20.INSTANCE);
		accessToken = new OAuth2AccessToken(aToken);
		json = new Gson();
		secret = SpreadSheetsDropboxServer.secret;
		updateMap();
	}

	@Override
	public String createSpreadsheet(long version, Spreadsheet sheet, String password, int first) {
		if (badSheet(sheet) || password == null || wrongPassword(sheet.getOwner(), password))
			throw new WebApplicationException(Status.BAD_REQUEST);

		synchronized (this) {
			var sheetId = sheet.getOwner() + "-" + counter.getAndIncrement() + DOMAIN;
			sheet.setSheetId(sheetId);
			sheet.setSheetURL(String.format("%s/%s",
					"https://" + IP.hostAddress() + ":" + SpreadSheetsDropboxServer.PORT + "/rest/spreadsheets",
					sheetId)); //cuidado ao fazer url
			sheet.setSharedWith(ConcurrentHashMap.newKeySet());

			updateOrOverwriteSpreadsheet(sheetId, sheet, "add");
			userSheets.computeIfAbsent(sheet.getOwner(), (k) -> ConcurrentHashMap.newKeySet()).add(sheetId);

			return sheetId;
		}
	}

	@Override
	public void deleteSpreadsheet(long version, String sheetId, String password, int first) {
		if (badParam(sheetId))
			throw new WebApplicationException(Status.BAD_REQUEST);

		Spreadsheet s = downloadSpreadsheet(sheetId);
		if (s == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (badParam(password) || wrongPassword(s.getOwner(), password))
			throw new WebApplicationException(Status.FORBIDDEN);
		else {
			delSpreadsheet(sheetId);
			userSheets.computeIfAbsent(s.getOwner(), (k) -> ConcurrentHashMap.newKeySet()).remove(sheetId);
		}

	}

	@Override
	public Spreadsheet getSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (badParam(sheetId) || badParam(userId))
			throw new WebApplicationException(Status.BAD_REQUEST);

		Spreadsheet s = downloadSpreadsheet(sheetId);
		//pode ter synchronized
		if (s == null || userId == null || getUser(userId) == null)
			throw new WebApplicationException(Status.NOT_FOUND);

		if (badParam(password) || wrongPassword(userId, password) || !s.hasAccess(userId, DOMAIN))
			throw new WebApplicationException(Status.FORBIDDEN);

		return s;
	}

	@Override
	public String[][] getSpreadsheetValues(long version, String sheetId, String userId, String password, int first) {
		if (badParam(sheetId) || badParam(userId))
			throw new WebApplicationException(Status.BAD_REQUEST);

		Spreadsheet s = downloadSpreadsheet(sheetId);
		if (s == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (badParam(password) || wrongPassword(userId, password) || !s.hasAccess(userId, DOMAIN))
			throw new WebApplicationException(Status.FORBIDDEN);

		String[][] values = getComputedValues(sheetId);

		if (values == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		return values;
	}

	@Override
	public void updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password, int first) {
		if (badParam(sheetId) || badParam(userId) || badParam(cell) || badParam(rawValue))
			throw new WebApplicationException(Status.BAD_REQUEST);

		Spreadsheet s = getSpreadsheet(version, sheetId, userId, password,0);
		if (s == null) { //pode ser ambiguo
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		if (badParam(password) || wrongPassword(userId, password))
			throw new WebApplicationException(Status.FORBIDDEN);

		s.setCellRawValue(cell, rawValue);

		synchronized (this) {
			updateOrOverwriteSpreadsheet(sheetId, s, "overwrite");
		}
		sheetValuesCache.invalidate(sheetId);
	}

	@Override
	public void shareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (badParam(sheetId) || badParam(userId))
			throw new WebApplicationException(Status.BAD_REQUEST);
		
		Spreadsheet s = downloadSpreadsheet(sheetId);
		
		if (s == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		if (badParam(password) || wrongPassword(s.getOwner(), password))
			throw new WebApplicationException(Status.FORBIDDEN);

		if (!s.getSharedWith().add(userId)) {
			throw new WebApplicationException(Status.CONFLICT);
		}
		synchronized (this) {
			updateOrOverwriteSpreadsheet(sheetId, s, "overwrite");
		}
	}

	@Override
	public void unshareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (badParam(sheetId) || badParam(userId))
			throw new WebApplicationException(Status.BAD_REQUEST);

		Spreadsheet s = downloadSpreadsheet(sheetId);
		if (s == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (badParam(password) || wrongPassword(s.getOwner(), password))
			throw new WebApplicationException(Status.FORBIDDEN);

		if (!s.getSharedWith().remove(userId))
			throw new WebApplicationException(Status.NOT_FOUND);

		synchronized (this) {
			updateOrOverwriteSpreadsheet(sheetId, s, "overwrite");
		}
		sheetValuesCache.invalidate(sheetId);

	}

	@Override
	public void deleteSpreadsheets(long version, String userId, int first) {
		synchronized(this) {
			var _userSheets = userSheets.get(userId);
			for(String s: _userSheets) {
				delSpreadsheet(s);
			}
			_userSheets.clear();
		}
	}
	
	

	@Override
	public String[][] fetchSpreadsheetValues(long version, String sheetId, String userId, String secret, int first) {
		if (badParam(sheetId) || badParam(userId))
			throw new WebApplicationException(Status.BAD_REQUEST);
		Spreadsheet s = downloadSpreadsheet(sheetId);
		if (s == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (!s.hasAccess(userId, DOMAIN))
			throw new WebApplicationException(Status.FORBIDDEN);
		
		String [][] values = getComputedValues(sheetId);
		if (values == null)
			throw new WebApplicationException(Status.BAD_REQUEST);
		return values;
	}

	private boolean badSheet(Spreadsheet sheet) {
		return sheet == null || !sheet.isValid();
	}

	private boolean wrongPassword(String userId, String password) {
		var user = getUser(userId);
		return user == null || !user.getPassword().equals(password);
	}

	private String url2Id(String url) {
		int i = url.lastIndexOf('/');
		return url.substring(i + 1);
	}

	private User getUser(String userId) {
		try {
			return users.get(userId);
		} catch (Exception x) {
			x.printStackTrace();
			return null;
		}
	}

	private boolean badParam(String str) {
		return str == null || str.length() == 0;
	}
	
	private Spreadsheet downloadSpreadsheet (String sheetId) {
		OAuthRequest download = new OAuthRequest(Verb.POST, DOWNLOAD_URL);
		download.addHeader("Content-Type", JSON_CONTENT_TYPE_OCTET);
		download.addHeader("Dropbox-API-Arg",
				json.toJson(new DeleteFolders("/" + Domain.get() + "/" + sheetId + ".txt")));
		service.signRequest(accessToken, download);

		Response r = common(download);
		Spreadsheet s = null;
		if (r != null) {
			try {
				s = json.fromJson(r.getBody(), Spreadsheet.class);
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
		}
		return s;
	}
	
	private void delSpreadsheet(String sheetId) {
		OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_URL);
		delete.addHeader("Content-Type", JSON_CONTENT_TYPE);
		delete.setPayload(json.toJson(new DeleteFolders("/" + Domain.get() + "/" + sheetId + ".txt")));
		service.signRequest(accessToken, delete);
		common(delete); //esta chamda ao common pode ter de ser verificada
	}
	
	private void updateOrOverwriteSpreadsheet(String sheetId, Spreadsheet sheet, String mode) {
		OAuthRequest upload = new OAuthRequest(Verb.POST, UPLOAD_URL);
		upload.addHeader("Content-Type", JSON_CONTENT_TYPE_OCTET);
		upload.addHeader("Dropbox-API-Arg",
				json.toJson(new Upload("/" + Domain.get() + "/" + sheetId + ".txt", mode)));
		upload.setPayload(json.toJson(sheet));
		service.signRequest(accessToken, upload);
		common(upload); 
	}
	
	private List<String> execute(String directoryName) {
		List<String> directoryContents = new ArrayList<String>();
		
		OAuthRequest listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_URL);
		listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
		listDirectory.setPayload(json.toJson(new ListFolderArgs(directoryName, false)));
		
		service.signRequest(accessToken, listDirectory);
		
		Response r = null;
		
		try {
			while(true) {
				r = service.execute(listDirectory);
				
				if(r.getCode() != 200) {
					System.err.println("Failed to list directory '" + directoryName + "'. Status " + r.getCode() + ": " + r.getMessage());
					System.err.println(r.getBody());
					return null;
				}
				
				ListFolderReturn reply = json.fromJson(r.getBody(), ListFolderReturn.class);
				
				for(FolderEntry e: reply.getEntries()) {
					directoryContents.add(e.get("name").toString());
				}
				
				if(reply.has_more()) {
					//There are more elements to read, prepare a new request (now a continuation)
					listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_CONTINUE_URL);
					listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
					//In this case the arguments is just an object containing the cursor that was returned in the previous reply.
					listDirectory.setPayload(json.toJson(new ListFolderContinueArgs(reply.getCursor())));
					service.signRequest(accessToken, listDirectory);
				} else {
					break; //There are no more elements to read. Operation can terminate.
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
			
		return directoryContents;
	}
	
	private void updateMap() {
		//Listar a diretoria de forma a poder fazer reset as folhas dos users
		List<String> l = execute("/"+Domain.get());
		if ( l != null) {
			for(String s: l) {
				String[] aux = s.split(".txt");
				String sheetId = aux[0];
				String[] aux2= sheetId.split("-");
				String userId = aux2[0];
				userSheets.computeIfAbsent(userId, (k) -> ConcurrentHashMap.newKeySet()).add(sheetId);
			}
		}
		
	}

	private Response common(OAuthRequest request) {
		Response r = null;

		try {
			r = service.execute(request);
		} catch (Exception e) {
			e.printStackTrace();
			return r;
		}

		if (r.getCode() == 200) {
			return r;
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return null;
		}
	}

	class SpreadsheetAdaptor implements AbstractSpreadsheet {

		final Spreadsheet sheet;

		SpreadsheetAdaptor(Spreadsheet sheet) {
			this.sheet = sheet;
		}

		@Override
		public int rows() {
			return sheet.getRows();
		}

		@Override
		public int columns() {
			return sheet.getColumns();
		}

		@Override
		public String sheetId() {
			return sheet.getSheetId();
		}

		@Override
		public String cellRawValue(int row, int col) {
			return sheet.getCellRawValue(row, col);
		}

		@Override
		public String[][] getRangeValues(String sheetURL, String range) {
			var x = resolveRangeValues(sheetURL, range, sheet.getOwner() + DOMAIN);
			Log.info("getRangeValues:" + sheetURL + " for::: " + range + "--->" + x);
			return x;
		}
	}

	private String[][] getComputedValues(String sheetId) {
		try {
			String[][] values = sheetValuesCache.getIfPresent(sheetId);

			if (values == null) {
				Spreadsheet s = downloadSpreadsheet(sheetId);
				values = engine.computeSpreadsheetValues(new SpreadsheetAdaptor(s));
				sheetValuesCache.put(sheetId, values);
			}
			return values;
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	public String[][] resolveRangeValues(String sheetUrl, String range, String userId) {
		String[][] values = null;
		Spreadsheet s = downloadSpreadsheet(url2Id(sheetUrl));
		if (s != null)
			values = getComputedValues(s.getSheetId());
		else {
			var m = SPREADSHEETS_URI_PATTERN.matcher(sheetUrl);
			if (m.matches()) {

				var uri = m.group(1);
				var sheetId = m.group(2);
				var result = SpreadsheetsClientFactory.with(uri).fetchSpreadsheetValues(0, sheetId, userId, secret,0);
				if (result.isOK()) {
					values = result.value();
					sheetValuesCache.put(sheetUrl, values);
				}
			}
			values = sheetValuesCache.getIfPresent(sheetUrl);
		}
		return values == null ? null : new CellRange(range).extractRangeValuesFrom(values);
	}
}
