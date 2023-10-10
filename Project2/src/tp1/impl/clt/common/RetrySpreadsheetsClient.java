package tp1.impl.clt.common;

import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;

public class RetrySpreadsheetsClient extends RetryClient implements Spreadsheets {

	final Spreadsheets impl;

	public RetrySpreadsheetsClient( Spreadsheets impl ) {
		this.impl = impl;
	}
	
	@Override
	public Result<String> createSpreadsheet(long version, Spreadsheet sheet, String password, int first) {
		return reTry( () -> impl.createSpreadsheet(version, sheet, password,first));
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return reTry( () -> impl.getSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public Result<Void> deleteSpreadsheet(long version, String sheetId, String password, int first) {
		return reTry( () -> impl.deleteSpreadsheet(version, sheetId, password,first));
	}

	@Override
	public Result<Void> shareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return reTry( () -> impl.shareSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public Result<Void> unshareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return reTry( () -> impl.unshareSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public Result<Void> updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password, int first) {
		return reTry( () -> impl.updateCell(version, sheetId, cell, rawValue, userId, password,first));
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(long version, String sheetId, String userId, String password, int first) {
		return reTry( () -> impl.getSpreadsheetValues(version, sheetId, userId, password,first));
	}

	@Override
	public Result<Void> deleteSpreadsheets(long version, String userId, int first) {
		return reTry( () -> impl.deleteSpreadsheets(version, userId,first));
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(long version, String sheetId, String userId, String secret, int first) {
		return reTry( () ->impl.fetchSpreadsheetValues(version, sheetId, userId, secret,first));
	}
}
