package tp1.impl.srv.rest;

import java.net.URI;
import java.util.List;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.clt.SpreadsheetsClientFactory;
import tp1.impl.discovery.Discovery;
import tp1.impl.srv.Domain;
import tp1.impl.srv.common.JavaSpreadsheets;
import tp1.impl.utils.IP;

@Singleton
@Path(RestSpreadsheets.PATH)
public class SpreadSheetsRepResources extends RestResource implements RestSpreadsheets {
	
	ZookeeperProcessor zk;
	Spreadsheets impl;
	long AVersion;
	
	public SpreadSheetsRepResources(){
		try {
			zk = new ZookeeperProcessor ("kafka:2181");
		} catch (Exception e) {
			e.printStackTrace();
		}
		impl =  new JavaSpreadsheets("https://" + IP.hostAddress() + ":" + SpreadSheetsRepServer.PORT + "/rest/spreadsheets");
		zk.addRep(SpreadSheetsRepServer.serverType, "https://" + IP.hostAddress() + ":" + SpreadSheetsRepServer.PORT + "/rest", Domain.get());
		AVersion = 0; //falta serializacao com classe da operacao
	}

	@Override
	public String createSpreadsheet(long version, Spreadsheet sheet, String password, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			//ter cuidado porque na altura de falhas a redirecao com cliente pode dar erro nas escritas
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).createSpreadsheet(version,sheet,password,1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets?password=" + password)).build());
			}
		}
		
		//pode ter que estar numa de sycronized para nao haver conflito de versoes
		AVersion++;
		return resultOrThrow(impl.createSpreadsheet(version, sheet, password, first));
	}
	
	@Override
	public void deleteSpreadsheet(long version, String sheetId, String password, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).deleteSpreadsheet(version, sheetId, password, 1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId + "?password=" + password)).build());
			}
		}
		AVersion++;
		resultOrThrow(impl.deleteSpreadsheet(version, sheetId, password, first));
		
	}

	@Override
	public Spreadsheet getSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (version > AVersion) {
			throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId)).build());
		}
		return resultOrThrow(impl.getSpreadsheet(version, sheetId, userId, password, first));
	}

	@Override
	public String[][] getSpreadsheetValues(long version, String sheetId, String userId, String password, int first) {
		if (version > AVersion) {
			throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId+ "/values?userId=" + userId + "&password=" + password)).build());
		}
		return resultOrThrow(impl.getSpreadsheetValues(version, sheetId, userId, password, first));
	}

	@Override
	public void updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).updateCell(version, sheetId, cell, rawValue, userId, password, 1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId + "/" + cell+ "?password=" + password + 
						"&userId=" + userId)).build());
			}
		}
		AVersion++;
		resultOrThrow(impl.updateCell(version, sheetId, cell, rawValue, userId, password, first));
		
	}

	@Override
	public void shareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).shareSpreadsheet(version, sheetId, userId, password, 1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId + "/share/" + userId + "?password=" + password)).build());
			}
		}
		AVersion++;
		resultOrThrow(impl.shareSpreadsheet(version, sheetId, userId, password, first));
		
	}

	@Override
	public void unshareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).unshareSpreadsheet(version, sheetId, userId, password, 1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId + "/share/" + userId + "?password=" + password)).build());
			}
		}
		AVersion++;
		resultOrThrow(impl.unshareSpreadsheet(version, sheetId, userId, password, first));
	}

	@Override
	public void deleteSpreadsheets(long version, String userId, int first) {
		if (SpreadSheetsRepServer.serverType.equals("1")) {
			URI [] u = Discovery.getInstance().findUrisOf(Domain.get() +":sheets" ,1);
			for (URI ur : u) {
				if (!ur.toString().equals(getUrlPrimary())){
					SpreadsheetsClientFactory.with(ur.toString()).deleteSpreadsheets(version, userId, 1);
				}
			}
		} else {
			if (first == 0) {
				throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + userId + "/sheets")).build());
			}
		}
		AVersion++;
		resultOrThrow(impl.deleteSpreadsheets(version, userId, first));
		
	}

	@Override
	public String[][] fetchSpreadsheetValues(long version, String sheetId, String userId, String secret, int first) {
		if (version > AVersion) {
			throw new WebApplicationException(Response.temporaryRedirect( URI.create(getUrlPrimary() +"/spreadsheets/" + sheetId+ "/fetch?userId=" + userId )).build());
		}
		return resultOrThrow(impl.fetchSpreadsheetValues(version, sheetId, userId, secret, first));
	}
	
	private String getUrlPrimary () {
		List<String> nodes = zk.getChildren("/" +Domain.get());
		String url = "";
		nodes.sort(null);
		url = zk.getUrl("/" +Domain.get() + "/" + nodes.get(0));
		//System.out.println("numero de sequencia do no primario" + nodes.get(0));
		return url;
	}

}
