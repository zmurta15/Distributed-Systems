package tp1.impl.clt.soap;

import java.net.URI;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.Spreadsheet;
import tp1.api.service.java.Result;
import tp1.api.service.java.Spreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.impl.utils.Url;

public class SoapSpreadsheetsClient extends SoapClient implements Spreadsheets {

	private SoapSpreadsheets impl;
	
	public SoapSpreadsheetsClient( URI uri ) {
		super( uri );
	}
	
	synchronized private SoapSpreadsheets impl() {
		if (impl == null) {
			var QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			//Adicionamos /spreadsheets pois esta a dar erro 
			var service = Service.create(Url.from(super.uri + "/spreadsheets"+ WSDL), QNAME);

			this.impl = service.getPort(tp1.api.service.soap.SoapSpreadsheets.class);
			super.setTimeouts((BindingProvider)impl);
		}
		return impl;
	}

	@Override
	public Result<String> createSpreadsheet(long version, Spreadsheet sheet, String password, int first) {
		return tryCatchResult( () -> impl().createSpreadsheet(sheet, password));
	}

	@Override
	public Result<Void> deleteSpreadsheet(long version, String sheetId, String password, int first) {
		return tryCatchVoid( () -> impl().deleteSpreadsheet(sheetId, password));
	}

	@Override
	public Result<Spreadsheet> getSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return tryCatchResult( () -> impl().getSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> shareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return tryCatchVoid( () -> impl().shareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> unshareSpreadsheet(long version, String sheetId, String userId, String password, int first) {
		return tryCatchVoid( () -> impl().unshareSpreadsheet(sheetId, userId, password));
	}

	@Override
	public Result<Void> updateCell(long version, String sheetId, String cell, String rawValue, String userId, String password, int first) {
		return tryCatchVoid( () -> impl().updateCell(sheetId, cell, rawValue, userId, password));
	}

	@Override
	public Result<String[][]> getSpreadsheetValues(long version, String sheetId, String userId, String password, int first) {
		return tryCatchResult( () -> impl().getSpreadsheetValues(sheetId, userId, password));
	}

	@Override
	public Result<Void> deleteSpreadsheets(long version, String userId, int first) {
		return tryCatchVoid( () -> impl().deleteSpreadsheets( userId ));
	}

	@Override
	public Result<String[][]> fetchSpreadsheetValues(long version, String sheetId, String userId, String secret, int first) {
		return tryCatchResult( () -> impl().fetchSpreadsheetValues(sheetId, userId, secret));
	}
}
