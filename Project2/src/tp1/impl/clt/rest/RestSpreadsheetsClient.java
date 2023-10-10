package tp1.impl.clt.rest;

import java.net.URI;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.rest.RestSpreadsheets;

public class RestSpreadsheetsClient extends RestClient implements Spreadsheets {
	private static final String PASSWORD = "password";
	private static final String USERID = "userId";
	private static final String VALUES = "/values";
	private static final String SHEETS = "/sheets";
	private static final String FETCH = "/fetch";

	public RestSpreadsheetsClient(URI serverUri) {
		super(serverUri, RestSpreadsheets.PATH);
	}

	@Override
	public Result<String> createSpreadsheet(long version, Spreadsheet sheet, String password,int first) {
		Response r = target
				.queryParam(PASSWORD, password)
				.request()
				.header("version", version)
				.header("first",first)
				.accept(  MediaType.APPLICATION_JSON)
				.post( Entity.entity(sheet, MediaType.APPLICATION_JSON));
		return super.responseContents(r, Status.OK, new GenericType<String>() {});
	}

	@Override
	public Result<Void> deleteSpreadsheet(long version, String sheetId, String password,int first) {
		Response r = target
				.path("/" + sheetId)
				.queryParam(PASSWORD, password)
				.request()
				.header("version", version)
				.header("first",first)
				.accept(  MediaType.APPLICATION_JSON)
				.delete();
		return super.responseContents(r, Status.NO_CONTENT, null);
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(long version, String sheetId, String userId, String password,int first) {
		Response r = target.path("/")
				.path(sheetId)
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.header("version", version)
				.header("first",first)
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		return super.responseContents(r, Status.OK, new GenericType<Spreadsheet>() {});
	}

	@Override
	public Result<Void> shareSpreadsheet(long version, String sheetId, String userId, String password,int first) {
		Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
				.queryParam(PASSWORD, password)
				.request()
				.header("version", version)
				.header("first",first)
				.post(Entity.json(""));
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<Void> unshareSpreadsheet(long version, String sheetId, String userId, String password,int first) {
		Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
				.queryParam(PASSWORD, password)
				.request()
				.header("version", version)
				.header("first",first)
				.delete();
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<Void> updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password,int first) {
		Response r = target.path(String.format("/%s/%s", sheetId, cell))
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.header("version", version)
				.header("first",first)
				.put(Entity.entity(rawValue, MediaType.APPLICATION_JSON));
		
		return verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(long version, String sheetId, String userId, String password,int first) {
		Response r = target
				.path(sheetId).path(VALUES)
				.queryParam(PASSWORD, password)
				.queryParam(USERID, userId)
				.request()
				.header("version", version)
				.header("first",first)
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		
		return super.responseContents(r, Status.OK, new GenericType<String[][]>() {});
	}

	@Override
	public Result<Void> deleteSpreadsheets( long version, String userId,int first) {
		Response r = target
				.path(userId).path(SHEETS)
				.request()
				.header("version", version)
				.header("first",first)
				.accept(  MediaType.APPLICATION_JSON)
				.delete();
		
		return super.verifyResponse(r, Status.NO_CONTENT);
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(long version, String sheetId, String userId, String secret,int first) {
		Response r = target.path( sheetId).path(FETCH)
				.queryParam(USERID, userId)
				.request()
				.header("version", version)
				.header("first",first)
				.header("secret", (Object) secret)
				.accept(  MediaType.APPLICATION_JSON)
				.get();
		
		return super.responseContents(r, Status.OK, new GenericType<String[][]>() {});
	}


}
