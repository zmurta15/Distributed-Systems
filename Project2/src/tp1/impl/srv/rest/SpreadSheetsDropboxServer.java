package tp1.impl.srv.rest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;
import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;

import tp1.impl.dropbox.args.*;
import tp1.impl.srv.Domain;

public class SpreadSheetsDropboxServer extends AbstractRestServer {
	public static final int PORT = 4567;
	public static final String SERVICE_NAME = "sheets";
	
	protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
	private static final String DELETE_FOLDERS_URL = "https://api.dropboxapi.com/2/files/delete_v2";
	
	private static OAuth20Service service;
	private static OAuth2AccessToken accessToken;
	protected static String APIKey;
	protected static String APISecret;
	protected static String aToken;
	protected static String secret;
	
	private static Gson json;
	
	
	private static Logger Log = Logger.getLogger(SpreadSheetsDropboxServer.class.getName());

	protected SpreadSheetsDropboxServer(int port) {
		super(Log, SERVICE_NAME, port);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.register( SpreadSheetsDropboxResources.class );
		config.register( GenericExceptionMapper.class );
		config.register( CustomLoggingFilter.class);
		
	}
	
	public static void main(String[] args) {
		Domain.set(args.length > 0 ? args[0] : "?");	
		//int port = args.length < 2 ? PORT : Integer.valueOf(args[1]);
		
		String bool = args[1];
		APIKey = args[2];
		APISecret = args[3];
		aToken = args[4];
		secret = args[5];

		Log.setLevel( Level.ALL );
		
		new SpreadSheetsDropboxServer(PORT).start();
		
		//se for para reinizicializar a dropbox
		if (bool.equals("true")) {
			removeFolders();
			createFolder();
		}
	}
	
	private static boolean createFolder () {
		service = new ServiceBuilder(APIKey).apiSecret(APISecret).build(DropboxApi20.INSTANCE);
		accessToken = new OAuth2AccessToken(aToken);
		json = new Gson();
		
		OAuthRequest createF = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
		createF.addHeader("Content-Type", JSON_CONTENT_TYPE);
		
		createF.setPayload(json.toJson(new CreateFolderV2Args("/" + Domain.get(), false)));
		
		service.signRequest(accessToken, createF);
		
		Response r = null;
		
		
		try {
			r = service.execute(createF);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(r.getCode() == 200) {
			return true;
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return false;
		}	
	}
	
	private static boolean removeFolders () {
		service = new ServiceBuilder(APIKey).apiSecret(APISecret).build(DropboxApi20.INSTANCE);
		accessToken = new OAuth2AccessToken(aToken);
		json = new Gson();
		
		OAuthRequest removeF = new OAuthRequest(Verb.POST, DELETE_FOLDERS_URL);
		removeF.addHeader("Content-Type", JSON_CONTENT_TYPE);
		
		removeF.setPayload(json.toJson(new DeleteFolders("/" + Domain.get())));
		
		service.signRequest(accessToken, removeF);
		
		Response r = null;
		
		try {
			r = service.execute(removeF);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(r.getCode() == 200) {
			return true;
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return false;
		}
	}

}
