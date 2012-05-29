package com.app.sharewifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ShareWifiActivity extends Activity {

	private WifiManager mWiFiManager;
	private ToggleButton togglebutton;
	
	//private ArrayList<String> AP = new ArrayList<String>();
	
	private ListView lv;
	private SimpleAdapter adapter;
	private HashMap<String, Object> row = new HashMap<String, Object>();
	private ArrayList<HashMap<String, ?>> data = new ArrayList<HashMap<String, ?>>();
	
	private IntentFilter intentFilter;
	private IntentFilter ifil;
	private BroadcastReceiver broadcastReceiver;
	private BroadcastReceiver br;
	private boolean receiverRegistered = false;
	private int netId;
	public boolean isConnectedOrFailed = false;
	public boolean isWEP = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
				
		/**
		 * Register Uncaught Exception Handler that saves the Force Close
		 * exceptions in a local file
		 */
		Thread.setDefaultUncaughtExceptionHandler(new FCExceptionHandler(this));

		/**
		 * Check if there are exceptions saved in a local file, and send/delete
		 * them
		 */
		checkBugs();

		// Initialize the WifiManager
		mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/**
		 * Initialize and set the toggle button state - on if the Wifi is on
		 */
		togglebutton = (ToggleButton) findViewById(R.id.toggle);

		if (mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
			togglebutton.setChecked(true);
		} else if (!mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING)
			togglebutton.setChecked(false);

		/**
		 * Set on click listener for the toggle button, change the state of the
		 * wifi on every toggle button state change
		 */
		togglebutton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (togglebutton.isChecked()) {
					if (!mWiFiManager.isWifiEnabled())
						if (mWiFiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
							mWiFiManager.setWifiEnabled(true);
				} else if (mWiFiManager.isWifiEnabled())
					if (mWiFiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLING)
						mWiFiManager.setWifiEnabled(false);
			}
		});

		/**
		 * First broadcast receiver that listens for changes in the connectivity
		 * manager. If the network is connected, add the AP to the configured
		 * networks list, or else, remove the newly added network because it
		 * can't connect, and unregister the receivers
		 */
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String reason = intent
						.getStringExtra(ConnectivityManager.EXTRA_REASON);
				NetworkInfo currentNetworkInfo = (NetworkInfo) intent
						.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

				String state = currentNetworkInfo.getDetailedState().name();
				Log.d("xxxX", "reason: " + reason);

				// CONNECTED or DISCONNECTED
				Log.d("xxxX", "state: " + state);
				if (currentNetworkInfo.getTypeName().equalsIgnoreCase("WIFI")) {
					if (currentNetworkInfo.isConnected()) {
						Toast.makeText(getApplicationContext(),
								"WIFI Connected...", Toast.LENGTH_SHORT).show();
						// If the phone has successfully connected to the AP,
						// save it!
						mWiFiManager.saveConfiguration();
						isConnectedOrFailed = true;
						unregisterReceiver(broadcastReceiver);
						unregisterReceiver(br);
						receiverRegistered = false;
					} else if (reason != null)
						Toast.makeText(getApplicationContext(), reason,
								Toast.LENGTH_SHORT).show();
					else if (state.equalsIgnoreCase("DISCONNECTED")) {
						// SupplicantState s =
						// mWiFiManager.getConnectionInfo().getSupplicantState();
						// NetworkInfo.DetailedState supstate =
						// WifiInfo.getDetailedStateOf(s);
						Toast.makeText(getApplicationContext(),
								"WIFI Disconnected!", Toast.LENGTH_SHORT)
								.show();
						mWiFiManager.removeNetwork(netId);
						isConnectedOrFailed = true;
						unregisterReceiver(broadcastReceiver);
						unregisterReceiver(br);
						receiverRegistered = false;

					}
				}
				Log.d("xxx",
						reason + " *** " + currentNetworkInfo.getExtraInfo());
			}
		};

		intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(ConnectivityManager.EXTRA_REASON);

		/**
		 * Second broadcast receiver that listens for supplicant changes and
		 * detects if there was an error in the attempt to connect to a wifi
		 * access point. Also deletes the network from current configured
		 * networks if there was an error
		 */
		ifil = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		br = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				boolean error = false;
				if (intent.getAction().equals(
						WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
					error = intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR);

				}
				if (error) {
					Log.d("xxxX",
							"Imaaaaa:  "
									+ intent.getStringExtra(WifiManager.EXTRA_SUPPLICANT_ERROR));
					Log.d("xxxX", "Error: ");
					Toast.makeText(getApplicationContext(),
							"WIFI Disconnected! The password may be incorrect",
							Toast.LENGTH_SHORT).show();
					mWiFiManager.removeNetwork(netId);
					isConnectedOrFailed = true;
					unregisterReceiver(broadcastReceiver);
					unregisterReceiver(br);
					receiverRegistered = false;
				}
			}
		};
		
		/**
		 * Initialize and set the on click listener to the scan button that will
		 * be used for scanning for matching APs (comparing the list from the
		 * application and currently available networks from the AP scan)
		 */
		Button scan = (Button) findViewById(R.id.scan);
		scan.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if (mWiFiManager.isWifiEnabled()) {
					getAvailableAPs();
				} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
					Toast.makeText(getApplicationContext(),
							"Please wait a bit until your WiFi is enabled!",
							Toast.LENGTH_SHORT).show();
				} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
						|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
					Toast.makeText(getApplicationContext(),
							"Please enable Your WiFi!", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
		
		//TODO init the list view
		
		lv = (ListView) findViewById(R.id.accesspointslist);
		lv.setTextFilterEnabled(true);

		Collections.sort(data, new MapComparator("Name"));

		adapter = new SimpleAdapter(this, data, R.layout.listitem,
				new String[] { "Id", "Name", "BSSID", "Signal", "Type", "Button" },
				new int[] { R.id.apID, R.id.name, R.id.bssid, R.id.signal, R.id.wifitype,
						R.id.connect }) {

			@Override
			public View getView(final int position, View convertView,
					ViewGroup parent) {

				if (convertView == null) {
					LayoutInflater infalInflater = (LayoutInflater) getApplicationContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = infalInflater.inflate(R.layout.listitem,
							null);
				}
				ImageView connect = (ImageView) convertView
						.findViewById(R.id.connect);
				connect.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String apID = data.get(position).get("Id") + "";
						Toast.makeText(getApplicationContext(), apID, Toast.LENGTH_SHORT).show();
						System.gc();
						//TODO
						//data.clear();
						//row.clear();
						//adapter = null;
					}
				});
				return super.getView(position, convertView, parent);
			}
		};

		lv.setAdapter(adapter);
	}

	/**
	 * on destroy of the activity, unregister the broadcast receivers if there
	 * are some left
	 */
	@Override
	protected void onDestroy() {
		if (receiverRegistered) {
			if (broadcastReceiver != null) {
				unregisterReceiver(broadcastReceiver);
				unregisterReceiver(br);
				receiverRegistered = false;
			}
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		if (mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
			togglebutton.setChecked(true);
		} else if (!mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING)
			togglebutton.setChecked(false);

		super.onResume();
	}

	/**
	 * Check if there is an internet connection (mobile or wifi) established for
	 * syncing and adding a new wifi AP to the web service
	 * 
	 * @return
	 */
	public boolean HaveNetworkConnection() {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}

	/**
	 * Create an alert dialog that redirects you to the internet options on the
	 * phone, so you can enable an internet connection
	 */
	public void createInternetDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your internet connection is disabled! Please enable WiFi, or mobile internet")
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.app_name)
				.setCancelable(false)
				.setPositiveButton("Internet options",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showNetOptions();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Start the wireless settings activity
	 */
	public void showNetOptions() {
		Intent netOptionsIntent = new Intent(
				android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		this.startActivity(netOptionsIntent);
	}

	/**
	 * Get the currently available APs from the scan, and add them to an array
	 * list
	 */
	public void getAvailableAPs() {

		data.clear();
		row.clear();
		
		mWiFiManager.startScan();
		List<ScanResult> mScanResults = mWiFiManager.getScanResults();
		if (mScanResults != null)
			for (ScanResult sr : mScanResults) {
				
				row = new HashMap<String, Object>();
				row.put("Id", sr.BSSID);
				row.put("Name", sr.SSID);
				row.put("BSSID", sr.BSSID);
				row.put("Signal", "Signal strength: "+sr.level+"dBm");
				if (sr.capabilities.contains("WEP")) {
					row.put("Type", R.drawable.wep);
				} else if (sr.capabilities.contains("WPA")) {
					row.put("Type", R.drawable.wpa);
				} else {
					row.put("Type", R.drawable.open);
				}
				row.put("Button", R.drawable.connect);
				data.add(row);
			}
		Collections.sort(data, new MapComparator("Name"));
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Get the AP parameters from the string parameter AP passed to this
	 * function, check the existing network configurations, and search for
	 * matchings. If there is a math, nothing is done, because the AP we're
	 * trying to connect to is already configured, else add the wifi
	 * configuration, and try to connect to the AP, and register the broadcast
	 * receivers to listen for wifi status changes.
	 * 
	 * @param AP
	 */
	public void connectTo(String AP) {

		boolean exists = false;
		String bssid = AP.substring(AP.indexOf("BSSID: ") + 7,
				AP.indexOf("\nPassword"));
		String psk = AP.substring(AP.indexOf("Password: ") + 10,
				AP.indexOf("\nWEP"));
		String ssid = AP.substring(AP.indexOf("AP name: ") + 9,
				AP.indexOf("\nBSSID"));
		if (AP.contains("WEP:true"))
			isWEP = true;
		else
			isWEP = false;

		// List available networks
		List<WifiConfiguration> configs = mWiFiManager.getConfiguredNetworks();
		for (WifiConfiguration config : configs) {

			Log.d("xxx", config.SSID + " ?= " + ssid);
			if (config.SSID.equalsIgnoreCase("\"" + ssid + "\"")) {
				exists = true;
			}
		}
		Log.d("xxx", "bssid: " + bssid + " psk: " + psk + "*");

		if (!exists) {

			WifiConfiguration wifiConfig = new WifiConfiguration();
			wifiConfig.SSID = "\"" + ssid + "\"";
			wifiConfig.BSSID = bssid;
			if (isWEP) {
				wifiConfig.wepKeys[0] = "\"" + psk + "\"";
			} else
				wifiConfig.preSharedKey = "\"" + psk + "\"";
			wifiConfig.status = WifiConfiguration.Status.ENABLED;

			mWiFiManager.setWifiEnabled(true);
			netId = mWiFiManager.addNetwork(wifiConfig);
			mWiFiManager.enableNetwork(netId, true);

			registerReceiver(broadcastReceiver, intentFilter);
			registerReceiver(br, ifil);
			receiverRegistered = true;

			new isConnected().execute();
		} else
			Toast.makeText(getApplicationContext(),
					"Network is already configured!", Toast.LENGTH_SHORT)
					.show();
	}

	/**
	 * Show a progress dialog, while the status of the wifi is either CONNECTED,
	 * DISCONNECTED or there was an error in the process
	 * 
	 * @author drakuwa
	 * 
	 */
	public class isConnected extends AsyncTask<String, Void, String> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(ShareWifiActivity.this);
			dialog.setTitle("Connecting!");
			dialog.setMessage("Please wait..");
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			dialog.show();
		}

		protected String doInBackground(String... vlezni) {
			while (true) {
				if (isConnectedOrFailed) {
					isConnectedOrFailed = false;
					break;
				}
			}
			return "";
		}

		public void onPostExecute(String result) {
			// Remove the progress dialog.
			dialog.dismiss();
		}
	}

	/**
	 * A function that checks the existence of stack.trace file and calls a
	 * function alert for sending/deleting it
	 */
	private void checkBugs() {

		File file = new File("/data/data/com.app.wifipass/files/stack.trace");
		if (file.exists()) {

			String line = "";
			String trace = "";
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								ShareWifiActivity.this
										.openFileInput("stack.trace")));
				while ((line = reader.readLine()) != null) {
					trace += line + "\n";
				}
			} catch (FileNotFoundException fnfe) {
				// ...
			} catch (IOException ioe) {
				// ...
			}

			syncExceptionsAlert(trace);
		}
	}

	/**
	 * A function that shows an AlertDialog for deleting/sending the stack.trace
	 * file to the developers email
	 * 
	 * @param trace
	 */
	public void syncExceptionsAlert(final String trace) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"There are unsent bugs, report them via email to the developer, or delete them? ")
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.app_name)
				.setCancelable(true)
				.setPositiveButton("Report",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent sendIntent = new Intent(
										Intent.ACTION_SEND);
								String subject = "Bug report";
								String body = "Mail bugs to drakuwa@gmail.com: "
										+ "\n\n" + trace + "\n\n";

								sendIntent.putExtra(Intent.EXTRA_EMAIL,
										new String[] { "drakuwa@gmail.com" });
								sendIntent.putExtra(Intent.EXTRA_TEXT, body);
								sendIntent.putExtra(Intent.EXTRA_SUBJECT,
										subject);
								sendIntent.setType("message/rfc822");

								ShareWifiActivity.this.startActivity(Intent
										.createChooser(sendIntent, "Title:"));

								ShareWifiActivity.this
										.deleteFile("stack.trace");
							}
						});
		builder.setNegativeButton("Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						File file = new File(
								"/data/data/com.app.wifipass/files/stack.trace");
						if (file.exists()) {
							ShareWifiActivity.this
									.deleteFile("stack.trace");
						}
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	class MapComparator implements Comparator<Map<String, ?>> {
		private final String key;

		public MapComparator(String key) {
			this.key = key;
		}

		public int compare(Map<String, ?> first, Map<String, ?> second) {
			//Null checking, both for maps and values
			String firstValue = (String) first.get(key);
			String secondValue = (String) second.get(key);
			return firstValue.compareTo(secondValue);
		}
	}
}