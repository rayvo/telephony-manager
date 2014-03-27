package com.javatpoint.telephonymanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AndroidRESTClientActivity extends Activity {

	private static final String SERVICE_URL = "http://223.194.199.18:80/whereami-server/webapi/location";
	// private static final String SERVICE_URL =
	// "http://10.12.9.231:80/whereami-server/webapi/location";

	private static final String TAG = "AndroidRESTClientActivity";
	TextView textView;

	private String latCell = "E";
	private String lonCell = "E";
	private String latGPS = "0";
	private String lonGPS = "0";

	private String lat = "E";
	private String lon = "E";

	private String result = "";

	private String log = "";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ocr_result);
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(Calendar.getInstance().getTime());
		log = timeStamp;
		Intent intent = getIntent();
		latCell = intent.getStringExtra("LAT");
		lonCell = intent.getStringExtra("LON");
		log = log + "," + latCell + "," + lonCell;

		String query = intent.getStringExtra("QUERY");

		String tmpQ = query.replace("%20", " ");
		log = log + "," + query;
		result = "Cell Lat: " + latCell;
		result = result + "\nCell Lon: " + lonCell;
		result = result + "\nQuery: " + query;

		textView = (TextView) findViewById(R.id.txtResult);
		Button btn = (Button) findViewById(R.id.saveBtn);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				logResult();
			}
		});

		// query = "SUNY Korea";

		/*
		 * lat = "37.592075"; lon = "126.683438"; query = "신토오리" + "&" + "커피베이";
		 */

		GPSTracker gps = new GPSTracker(this);
		if (gps.canGetLocation()) {
			latGPS = String.valueOf(gps.getLatitude());
			lonGPS = String.valueOf(gps.getLongitude());
		}
		result = result + "\n\nLat GPS: " + latGPS;
		result = result + "\nLon GPS: " + lonGPS;
		log = log + "," + latGPS;
		log = log + "," + lonGPS;

		new NetworkTask().execute(new String[] { query });

		textView.setText(result);
		gps.stopUsingGPS();

		getLocation(query);

	}

	protected void logResult() {
		// TODO Auto-generated method stub
		appendLog(log);
		finish();
	}

	public void appendLog(String text) {
		File logFile = new File("sdcard/log.txt");
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
					true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class NetworkTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			// Create a connection to some 'hidden' Google-API
			String query = params[0];
			query = query.replace(" ", "%20");
			String url = SERVICE_URL + "/" + latCell + "/" + lonCell + "/"
					+ query;
			System.out.println(url);
			String rawResult = "";
			try {
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 10000;
				HttpConnectionParams.setConnectionTimeout(httpParameters,
						timeoutConnection);
				int timeoutSocket = 10000;
				HttpConnectionParams
						.setSoTimeout(httpParameters, timeoutSocket);
				DefaultHttpClient httpClient = new DefaultHttpClient(
						httpParameters);
				HttpGet getRequest = new HttpGet(url);
				getRequest.addHeader("accept", "text/plain");
				System.out.println("1");
				HttpResponse response = httpClient.execute(getRequest);
				System.out.println("2");
				rawResult = getResult(response).toString();
				httpClient.getConnectionManager().shutdown();

				String[] ss = rawResult.split(",");
				int flag = (int) Integer.parseInt(ss[0]);
				if (flag == 1) {
					lat = ss[1];
					lon = ss[2];
					result = result + "\n\n  Done Successfully:";
					result = result + "\nLat: " + lat;
					result = result + "\nLon: " + lon;
					log = log + "," + lat;
					log = log + "," + lon;
					log = log + "\n";					
				}
				try {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {

							TextView txtView = (TextView) AndroidRESTClientActivity.this
									.findViewById(R.id.txtResult);
							txtView.setText(result);

						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			System.out.println("lat,lon=" + lat + "," + lon);

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub

			TextView txtView = (TextView) AndroidRESTClientActivity.this
					.findViewById(R.id.txtResult);
			txtView.setText(result);

			super.onProgressUpdate(values);
		}

	}

	public void getLocation(String query) {

		/*
		 * String url = SERVICE_URL + "/" + lat + "/" + lon + "/" + query;
		 * String rawResult = ""; try { HttpParams httpParameters = new
		 * BasicHttpParams(); int timeoutConnection = 5000;
		 * HttpConnectionParams.setConnectionTimeout(httpParameters,
		 * timeoutConnection); int timeoutSocket = 5000;
		 * HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		 * DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		 * HttpGet getRequest = new HttpGet(url); getRequest.addHeader("accept",
		 * "text/plain");
		 * 
		 * HttpResponse response = httpClient.execute(getRequest);
		 * 
		 * rawResult = getResult(response).toString();
		 * httpClient.getConnectionManager().shutdown();
		 * 
		 * } catch (Exception e) { System.out.println(e.getMessage()); }
		 * String[] ss = rawResult.split(","); int flag = (int)
		 * Integer.parseInt(ss[0]); if (flag == 1) { lat =ss[1]; lon =ss[2]; }
		 * else { lat = "E"; lon = "E"; }
		 * 
		 * GPSTracker gps = new GPSTracker(this); if(gps.canGetLocation()){
		 * latGPS = String.valueOf(gps.getLatitude()); lonGPS =
		 * String.valueOf(gps.getLongitude()); }
		 * 
		 * result = result + "\n\nLat GPS: " + latGPS; result = result +
		 * "\nLon GPS: " + lonGPS; result = result + "\n\n Lat: " + lat; result
		 * = result + "\nLon: " + lon; log = log + "," + latGPS; log = log + ","
		 * + lonGPS; log = log + "," + lat; log = log + "," + lon; log = log +
		 * "\n"; textView.setText(result); gps.stopUsingGPS();
		 */

	}

	private StringBuilder getResult(HttpResponse response)
			throws IllegalStateException, IOException {
		StringBuilder result = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				(response.getEntity().getContent())), 1024);
		String output;
		while ((output = br.readLine()) != null)
			result.append(output);

		return result;
	}
}
