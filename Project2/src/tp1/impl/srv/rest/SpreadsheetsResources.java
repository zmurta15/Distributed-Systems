package tp1.impl.srv.rest;

import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.srv.common.JavaSpreadsheets;
import tp1.impl.utils.IP;

@Singleton
@Path(RestSpreadsheets.PATH)
public class SpreadsheetsResources extends RestResource implements RestSpreadsheets {
	private static Logger Log = Logger.getLogger(SpreadsheetsResources.class.getName());

	final Spreadsheets impl;

	public SpreadsheetsResources() {
		var uri = String.format("https://%s:%d/rest%s", IP.hostAddress(), SpreadsheetsRestServer.PORT, PATH);
		impl = new JavaSpreadsheets(uri);
	}

	public String createSpreadsheet(long version, Spreadsheet sheet, String password, int first) {
		Log.info(String.format("REST createSpreadsheet: sheet = %s\n", sheet));

		return super.resultOrThrow(impl.createSpreadsheet(version, sheet, password,first));
	}

	@Override
	public void deleteSpreadsheet(long version, String sheetId, String password, int first) {
		Log.info(String.format("REST deleteSpreadsheet: sheetId = %s\n", sheetId));

		super.resultOrThrow(impl.deleteSpreadsheet(version, sheetId, password,first));
	}

	@Override
	public Spreadsheet getSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		Log.info(String.format("REST getSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		return super.resultOrThrow(impl.getSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public void shareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		Log.info(String.format("REST shareSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		super.resultOrThrow(impl.shareSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public void unshareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		Log.info(String.format("REST unshareSpreadsheet: sheetId = %s, userId = %s\n", sheetId, userId));

		super.resultOrThrow(impl.unshareSpreadsheet(version, sheetId, userId, password,first));
	}

	@Override
	public void updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password, int first) {
		Log.info(String.format("REST updateCell: sheetId = %s, cell= %s, rawValue = %s, userId = %s\n", sheetId, cell,
				rawValue, userId));

		super.resultOrThrow(impl.updateCell(version, sheetId, cell, rawValue, userId, password,first));
	}

	@Override
	public String[][] getSpreadsheetValues(long version, String sheetId, String userId, String password, int first) {
		Log.info(String.format("REST getSpreadsheetValues: sheetId = %s, userId = %s\n", sheetId, userId));

		return super.resultOrThrow(impl.getSpreadsheetValues(version, sheetId, userId, password,first));
	}

	@Override
	public void deleteSpreadsheets(long version, String userId, int first) {
		Log.info(String.format("REST deleteSpreadsheets: userId = %s\n", userId));

		super.resultOrThrow(impl.deleteSpreadsheets(version, userId,first));
	}

	@Override
	public String[][] fetchSpreadsheetValues(long version, String sheetId, String userId, String secret, int first) {
		Log.info(String.format("REST fetchSpreadsheetValues: sheetId = %s, userId: %s\n", sheetId, userId));
		
		return super.resultOrThrow( impl.fetchSpreadsheetValues(version, sheetId, userId, secret,first));
	}
}
