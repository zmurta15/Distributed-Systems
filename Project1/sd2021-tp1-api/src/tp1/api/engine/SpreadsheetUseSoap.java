package tp1.api.engine;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.util.CellRange;

public class SpreadsheetUseSoap implements AbstractSpreadsheet {
	
	public final static String SHEETS_WSDL = "/spreadsheets/?wsdl";
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 15000;
	public final static int REPLY_TIMEOUT = 600;
	
	private Spreadsheet sheet;
	
	public SpreadsheetUseSoap(Spreadsheet s) {
		this.sheet = s;
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
		try {
			return sheet.getRawValues()[row][col];
		} catch (IndexOutOfBoundsException e) {
			return "#ERROR?";
		}
	}

	@Override
	public String[][] getRangeValues(String sheetURL, String range) {
		SoapSpreadsheets sheets = null;
		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create( new URL(sheetURL + SHEETS_WSDL), QNAME );
			sheets = service.getPort( tp1.api.service.soap.SoapSpreadsheets.class);
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT,CONNECTION_TIMEOUT);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
		
		short retries = 0;
		boolean success = false;
		String result[][] = null;
		String[] temp = sheetURL.split("/");
		String sheetIdTemp = temp[5];

		while (!success && retries < MAX_RETRIES) {

			try {
				result = sheets.getSpreadImport(sheetIdTemp);
				success = true;

			} catch (SheetsException e) {
				System.out.println("Cound not get sheet: " + e.getMessage());
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
		CellRange cell = new CellRange(range);
		
		return cell.extractRangeValuesFrom(result);
	}

}
