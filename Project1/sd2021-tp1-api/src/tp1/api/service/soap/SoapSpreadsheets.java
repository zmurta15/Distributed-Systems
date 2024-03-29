package tp1.api.service.soap;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;

@WebService(serviceName=SoapSpreadsheets.NAME, targetNamespace=SoapSpreadsheets.NAMESPACE, endpointInterface=SoapSpreadsheets.INTERFACE)
public interface SoapSpreadsheets {
	
	static final String NAME = "spreadsheets";
	static final String NAMESPACE = "http://sd2021";
	static final String INTERFACE = "tp1.api.service.soap.SoapSpreadsheets";

	/**
	 * Creates a new spreadsheet. The sheetId and sheetURL are generated by the server.
	 * @param sheet - the spreadsheet to be created.
	 * @param password - the password of the owner of the spreadsheet.
	 * 
	 * @throws SheetsException otherwise
	 */
	String createSpreadsheet(Spreadsheet sheet, String password ) throws SheetsException;

	
	/**
	 * Deletes a spreadsheet.
	 * @param sheetId - the sheet to be deleted.
	 * @param password - the password of the owner of the spreadsheet.
	 * 
	 * @throws SheetsException otherwise
	 */
	void deleteSpreadsheet(String sheetId, String password) throws SheetsException;

	/**
	 * Retrieve a spreadsheet.
	 * 	
	 * @param sheetId - The  spreadsheet being retrieved.
	 * @param userId - The user performing the operation.
	 * @param password - The password of the user performing the operation.
	 * 
	 * @throws SheetsException otherwise
	 */
	Spreadsheet getSpreadsheet(String sheetId , String userId, String password) throws SheetsException;
		
	
	/**
	 * 
	 * Adds a new user to the list of shares of a spreadsheet.
	 * 
	 * @param sheetId - the sheet being shared.
	 * @param userId - the user that is being added to the list of shares.
	 * @param password - The password of the owner of the spreadsheet.
	 * 
	 * @throws SheetsException otherwise
	 */
	void shareSpreadsheet( String sheetId, String userId, String password) throws SheetsException;

	
	/**
	 * Removes a user from the list of shares of a spreadsheet.
	 * 
	 * @param sheetId - the sheet being shared.
	 * @param userId - the user that is being removed from the list of shares.
	 * @param password - The password of the owner of the spreadsheet.
	 * 
	 * @throws SheetsException otherwise
	 */
	void unshareSpreadsheet( String sheetId, String userId, String password) throws SheetsException;

	
	/**
	 * Updates the raw values of some cells of a spreadsheet. 
	 * 
	 * @param userId - The user performing the update.
	 * @param sheetId - the spreadsheet whose values are being retrieved.
	 * @param cell - the cell being updated
	 * @param rawValue - the new raw value of the cell
	 * @param password - the password of the owner of the spreadsheet
	 * 
	 * @throws SheetsException otherwise
	 **/
	void updateCell( String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException;

	
	/**
	 * Retrieves the calculated values of a spreadsheet.
	 * @param userId - The user requesting the values
	 * @param sheetId - the spreadsheet whose values are being retrieved.
	 * @param password - the password of the owner of the spreadsheet
	 * 
	 * @throws SheetsException otherwise
	 */
	String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException;
	
	/**
	 * Deletes the sheets from an user when the user is deleted 
	 * 
	 * @param userId - the id of the user that is going to be deleted
	 * @throws SheetsException
	 */
	void deleteSheetsFromUser(String userId) throws SheetsException;
	
	/**
	 * Returns the computed values of the spreadsheet
	 * 
	 * @param sheetId - the id of the spreadsheet to get the values
	 * @return computed values of the spreadsheet
	 * @throws SheetsException
	 */
	String[][] getSpreadImport(String sheetId) throws SheetsException;
}
