package com.javatpoint.telephonymanager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	TextView textView;
	private static final String LOG_TAG = "WHEREAMI-MainActivity";
	private static final String GOOGLE_URL = "http://www.google.com/glm/mmap";
	
	public static double lat = 0;
	public static double lon = 0;

	int cid = 0;
	int lac = 0;
	int mcc = 450; //Korea
	int mnc = 5;   //SK Telecom

	
	Intent restIntent = null;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = (TextView) findViewById(R.id.textView1);
		Button btn = (Button) findViewById(R.id.btn);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				postData();
			}
		});
		restIntent = new Intent(this, AndroidRESTClientActivity.class);
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		GsmCellLocation cell = null;

		String IMEINumber = tm.getDeviceId();
		String subscriberID = tm.getDeviceId();
		String SIMSerialNumber = tm.getSimSerialNumber();
		String networkCountryISO = tm.getNetworkCountryIso();
		String SIMCountryISO = tm.getSimCountryIso();
		String softwareVersion = tm.getDeviceSoftwareVersion();
		String voiceMailNumber = tm.getVoiceMailNumber();

		// Get the phone type
		String strphoneType = "";

		int phoneType = tm.getPhoneType();
		int networkType = tm.getNetworkType();
		switch (networkType) {
		case (TelephonyManager.NETWORK_TYPE_CDMA):
			//
			Log.d(LOG_TAG, "***NETWORK_TYPE_CDMA:");
			break;
		case (TelephonyManager.NETWORK_TYPE_LTE):
			//
			Log.d(LOG_TAG, "***NETWORK_TYPE_LTE:");
			break;

		}

		List<NeighboringCellInfo> neighbors = tm.getNeighboringCellInfo();
		if (neighbors != null && neighbors.size() > 0) {
			Log.d(LOG_TAG, "***# of neighbors:" + neighbors.size());
		} else {
			Log.d(LOG_TAG, "***neighbors:" + neighbors);
		}

		switch (phoneType) {
		case (TelephonyManager.PHONE_TYPE_CDMA):
			strphoneType = "CDMA";
			lat = ((CdmaCellLocation) tm.getCellLocation())
					.getBaseStationLatitude();
			lon = ((CdmaCellLocation) tm.getCellLocation())
					.getBaseStationLongitude();
			break;
		case (TelephonyManager.PHONE_TYPE_GSM):
			strphoneType = "GSM";

			break;
		case (TelephonyManager.PHONE_TYPE_NONE):
			strphoneType = "NONE";
			break;
		}

		// getting information if phone is in roaming
		boolean isRoaming = tm.isNetworkRoaming();

		//String info = "Cell tower location:\n";
		

		if (strphoneType.equals("GSM")) {
			cell = (GsmCellLocation) tm.getCellLocation();
			cid = cell.getCid();
			lac = cell.getLac();

			String networkOperator = tm.getNetworkOperator();
			String str_mcc = "";
			String str_mnc = "";
			
			if (networkOperator != null) {
				str_mcc = networkOperator.substring(0, 3);
				str_mnc = networkOperator.substring(3);
				Log.d(LOG_TAG, "mcc: " + str_mcc);
				Log.d(LOG_TAG, "mnc: " + str_mnc);
			}

		
			//textView.setText(textView.getText() + info);

			String str_cid = String.valueOf(cid);
			String str_lac = String.valueOf(lac);
			// String str_mcc = String.valueOf(cell.get);

			/*
			 * str_cid = "29021"; str_lac = "328";
			 */

			new NetworkTask().execute(new String[] { str_cid, str_lac, str_mcc, str_mnc });

		} else {
			
		}

	}

	private void WriteData(OutputStream out, int cid, int lac, int mcc, int mnc)
			throws IOException {

		DataOutputStream dataOutputStream = new DataOutputStream(out);
		dataOutputStream.writeShort(21);
		dataOutputStream.writeLong(0);
		dataOutputStream.writeUTF("en");
		dataOutputStream.writeUTF("Android");
		dataOutputStream.writeUTF("1.0");
		dataOutputStream.writeUTF("Web");
		dataOutputStream.writeByte(27);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		// GSM uses 4 digits while UTMS used 6 digits (HEX)
		if (cid > 65536) {
			dataOutputStream.writeInt(5);
		} else {
			dataOutputStream.writeInt(3);
		}
		dataOutputStream.writeUTF("");

		dataOutputStream.writeInt(cid);
		dataOutputStream.writeInt(lac);

		dataOutputStream.writeInt(mnc);
		dataOutputStream.writeInt(mcc);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.flush();
	}

	// AsyncTask to call web service
	private class NetworkTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {			
			// Create a connection to some 'hidden' Google-API
			try {
				URL url = new URL(GOOGLE_URL);
				URLConnection conn = url.openConnection();
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				httpConn.setRequestMethod("POST");
				httpConn.setDoOutput(true);
				httpConn.setDoInput(true);
				httpConn.connect();

				cid = Integer.parseInt(params[0]);
				lac = Integer.parseInt(params[1]);
				mcc = Integer.parseInt(params[2]);
				mnc = Integer.parseInt(params[3]);
				
				Log.d(LOG_TAG, "cid = " + cid + ", lac = " + lac);

				OutputStream outputStream = httpConn.getOutputStream();
				
				WriteData(outputStream, cid, lac, mcc, mnc);

				InputStream inputStream = httpConn.getInputStream();
				DataInputStream dataInputStream = new DataInputStream(
						inputStream);

				// ---interpret the response obtained---
				dataInputStream.readShort();
				dataInputStream.readByte();
				int code = dataInputStream.readInt();

				Log.d(LOG_TAG, "***code:" + code);
				if (code == 0) {
					lat = (double) dataInputStream.readInt() / 1000000D;
					lon = (double) dataInputStream.readInt() / 1000000D;

					Log.d(LOG_TAG, "***lat:" + lat + "  log" + lon);

					dataInputStream.readInt();
					dataInputStream.readInt();
					dataInputStream.readUTF();

					try {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String info = "Lat:" + lat;
								info += "\nLon:" + lon;
								
								restIntent.putExtra("LAT", String.valueOf(lat));
								restIntent.putExtra("LON", String.valueOf(lon));
								
								TextView txtView = (TextView) MainActivity.this
										.findViewById(R.id.textView1);
								txtView.setText(txtView.getText() + info);
								System.out.println("test");
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
					Log.d(LOG_TAG, "***subGPS true");
				} else {
					Log.d(LOG_TAG, "***subGPS false");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public void postData() {
		
		String query = createQuery();
		restIntent.putExtra("QUERY", query);
		startActivity(restIntent);
	}
	
	
		 

	private String createQuery() {
		String result = "";
		TextView txv = (TextView) findViewById(R.id.firstKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + txv.getText().toString().trim();
		}
		
		txv = (TextView) findViewById(R.id.secondKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + "&" + txv.getText().toString().trim();
		}
		
		txv = (TextView) findViewById(R.id.thirdKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + "&" + txv.getText().toString().trim();
		}
		
		txv = (TextView) findViewById(R.id.fourthKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + "&" + txv.getText().toString().trim();
		}
		
		txv = (TextView) findViewById(R.id.fifthKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + "&" + txv.getText().toString().trim();
		}
		
		txv = (TextView) findViewById(R.id.sixthKW);
		if (txv.getText().toString().trim().equals("")) {
			return result;
		} else {
			result = result + "&" + txv.getText().toString().trim();
		}

		return result;
	}

	
	
}
