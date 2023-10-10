package tp1.api.service.java;

import tp1.api.Spreadsheet;


public interface Spreadsheets {

	Result<String> createSpreadsheet(long version, Spreadsheet sheet, String password, int first);

	Result<Void> deleteSpreadsheet(long version, String sheetId, String password, int first);

	Result<Spreadsheet> getSpreadsheet(long version, String sheetId , String userId, String password, int first);
			
	Result<Void> shareSpreadsheet( long version, String sheetId, String userId, String password, int first);
	
	Result<Void> unshareSpreadsheet( long version, String sheetId, String userId, String password, int first);
	
	Result<Void> updateCell( long version, String sheetId, String cell, String rawValue, String userId, String password, int first);

	Result<String[][]> getSpreadsheetValues(long version, String sheetId, String userId, String password, int first);
		
	Result<Void> deleteSpreadsheets( long version, String userId, int first );
	
	Result<String[][]> fetchSpreadsheetValues( long version, String sheetId, String userId , String secret, int first);
}
