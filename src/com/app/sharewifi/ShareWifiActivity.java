package com.app.sharewifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ShareWifiActivity extends Activity {

	private WifiManager mWiFiManager;
	private ToggleButton togglebutton;
		
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
	
	private DBAdapter db = new DBAdapter(this);
	public Model model;
	private String password = "";
	
	//private Resources res = this.getResources();
	private String statuscon1;// = getResources().getString(R.string.statusconnected1);
	private String statuscon2;// = getResources().getString(R.string.statusconnected2);
	private String statuscon3;// = getResources().getString(R.string.statusconnected3);
	private String statusnotconnected;// = getResources().getString(R.string.status);
	private String wificonnected;// = getResources().getString(R.string.wificonnected);
	private String wifidisconnected;// = getResources().getString(R.string.wifidisconnected);
	private String wifidisconnected2;// = getResources().getString(R.string.wifidisconnected2);
	private String pleasewaitwifi;// = getResources().getString(R.string.pleasewaitwifi);
	private String pleasewait;// = getResources().getString(R.string.pleasewait);
	private String pleaseenablewifi;// = getResources().getString(R.string.pleaseenablewifi);
	private String connectto;// = getResources().getString(R.string.connectto);
	private String connecttt;// = getResources().getString(R.string.connect);
	private String enterpassword;// = getResources().getString(R.string.enterpassword);
	private String cancel;// = getResources().getString(R.string.cancel);
	private String signalstrength;// = getResources().getString(R.string.signalstrength);
	private String connecting;// = getResources().getString(R.string.connecting);
	private String alreadyconfigured;// = getResources().getString(R.string.alreadyconfigured);
	private String delete;// = getResources().getString(R.string.delete);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		try {
			db.open();
		} catch (Exception e) {
			// handle exception
		}
		
		statuscon1 = getResources().getString(R.string.statusconnected1);
		statuscon2 = getResources().getString(R.string.statusconnected2);
		statuscon3 = getResources().getString(R.string.statusconnected3);
		statusnotconnected = getResources().getString(R.string.status);
		wificonnected = getResources().getString(R.string.wificonnected);
		wifidisconnected = getResources().getString(R.string.wifidisconnected);
		wifidisconnected2 = getResources().getString(R.string.wifidisconnected2);
		pleasewaitwifi = getResources().getString(R.string.pleasewaitwifi);
		pleasewait = getResources().getString(R.string.pleasewait);
		pleaseenablewifi = getResources().getString(R.string.pleaseenablewifi);
		connectto = getResources().getString(R.string.connectto);
		connecttt = getResources().getString(R.string.connect);
		enterpassword = getResources().getString(R.string.enterpassword);
		cancel = getResources().getString(R.string.cancel);
		signalstrength = getResources().getString(R.string.signalstrength);
		connecting = getResources().getString(R.string.connecting);
		alreadyconfigured = getResources().getString(R.string.alreadyconfigured);
		delete = getResources().getString(R.string.delete);
		
		/**
		 * Initialize the model
		 */
		model = new Model(this);		
		
		/**
		 * Register Uncaught Exception Handler that saves the Force Close
		 * exceptions in a local file
		 */
		Thread.setDefaultUncaughtExceptionHandler(new FCExceptionHandler(this));

		/**
		 * Check if there are exceptions saved in a local file, and send/delete
		 * them
		 */
		model.checkBugs();

		// Initialize the WifiManager
		mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo w = mWiFiManager.getConnectionInfo();
		if(w.getBSSID()!=null){
			TextView status = (TextView)findViewById(R.id.status);
			status.setText(statuscon1 + w.getSSID() + statuscon2 +w.getBSSID() + statuscon3 + w.getLinkSpeed()+"MBps");
		}
		Log.d("xxx", w.getSSID()+"::"+w.getBSSID()+"::"+w.getLinkSpeed());

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
						{
							mWiFiManager.setWifiEnabled(false);
							if(data!=null&&row!=null&&adapter!=null){
								data.clear();
								row.clear();
								adapter.notifyDataSetChanged();
								TextView status = (TextView)findViewById(R.id.status);
								status.setText(statusnotconnected);
							}
						}
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
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
								wificonnected, Toast.LENGTH_SHORT).show();
						// If the phone has successfully connected to the AP,
						// save it!
						refreshStatus();
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
								wifidisconnected, Toast.LENGTH_SHORT)
								.show();
						mWiFiManager.removeNetwork(netId);
						isConnectedOrFailed = true;

						TextView status = (TextView)findViewById(R.id.status);
						status.setText(statusnotconnected);
						
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
							wifidisconnected2,
							Toast.LENGTH_SHORT).show();
					mWiFiManager.removeNetwork(netId);
					isConnectedOrFailed = true;
					
					TextView status = (TextView)findViewById(R.id.status);
					status.setText(statusnotconnected);
					
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
							pleasewaitwifi,
							Toast.LENGTH_SHORT).show();
				} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
						|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
					Toast.makeText(getApplicationContext(),
							pleaseenablewifi, Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
		
		//TODO init the list view
		
		lv = (ListView) findViewById(R.id.accesspointslist);
		lv.setTextFilterEnabled(true);

		Collections.sort(data, model.new MapComparator("Name"));

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
						
						final ArrayList<String> AP = new ArrayList<String>();
						AP.add(data.get(position).get("BSSID").toString());
						
						//does the configuration of this network exist
						boolean exists = false;
						WifiConfiguration existing = null;
						String ssid = data.get(position).get("Name").toString();
						
						// List available networks
						List<WifiConfiguration> configs = mWiFiManager.getConfiguredNetworks();
						for (WifiConfiguration config : configs) {

							Log.d("xxx", config.SSID + " ?= " + ssid);
							if (config.SSID.equalsIgnoreCase("\"" + ssid + "\"")) {
								existing = config;
								exists = true;
							}
						}
						//If the network is already configured, connect to it!
						if(exists){
							//Toast.makeText(getApplicationContext(),
							//		"Network is already configured!", Toast.LENGTH_SHORT)
							//		.show();
							if(existing!=null)connectOrDelete(existing);
						}
						else if(data.get(position).get("Type").equals(R.drawable.open)){
							AP.add("open-network");
					        AP.add(data.get(position).get("Name").toString());
					        AP.add(data.get(position).get("Type").toString());
					        connectTo(AP, true, false);
						} else {
							AlertDialog.Builder alert = new AlertDialog.Builder(ShareWifiActivity.this);
					        alert.setTitle(connectto+data.get(position).get("Name").toString());
					        alert.setMessage(enterpassword);

					        // Set an EditText view to get user input 
					        final EditText input = new EditText(ShareWifiActivity.this);
					        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					        alert.setView(input);

					        alert.setPositiveButton(connecttt, new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
					          Editable value = input.getText();
					          password = value.toString();
					          AP.add(password);
					          AP.add(data.get(position).get("Name").toString());
					          AP.add(data.get(position).get("Type").toString());
					          if(data.get(position).get("Type").equals(R.drawable.wep))
					        	  connectTo(AP, false, true);
					          else connectTo(AP, false, false);
					          dialog.dismiss();
					          }
					        });
					        alert.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
					          public void onClick(DialogInterface dialog, int whichButton) {
					            dialog.cancel();
					          }
					        });
					        alert.show();
						}
					}
				});
				return super.getView(position, convertView, parent);
			}
		};

		lv.setAdapter(adapter);
	}

	/**
	 * on destroy of the activity, unregister the broadcast receivers if there
	 * are some left, and close the db cursor
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
		if(db!=null){ 
			try {
				db.close();
			} catch (Exception e) {
				//handle exception
			}
		}
		super.onDestroy();
	}

	/**
	 * Refresh the toggle button state
	 */
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
				row.put("Signal", signalstrength+sr.level+"dBm");
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
		Collections.sort(data, model.new MapComparator("Name"));
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
	public void connectTo(ArrayList<String> AP, boolean isOpen, boolean isWEP) {

		//does the configuration of this network exist
		//boolean exists = false;
		//WifiConfiguration existing = null;
		
		String bssid = AP.get(0);
		password = AP.get(1);
		
		String ssid = AP.get(2);
		//if (AP.get(3).contains("WEP:true"))
		//if (AP.get(3).equals(R.drawable.wep))
		//{
		//	isWEP = true;
		//	Log.d("xxx", "WEP = True!");
		//	Log.d("xxx", "AP.get(3) = "+AP.get(3));
		//}
		//else
		//{
		//	isWEP = false;
		//	Log.d("xxx", "WEP = False!");
		//	Log.d("xxx", "AP.get(3) = "+AP.get(3));
		//}
		

		// List available networks
		/*
		List<WifiConfiguration> configs = mWiFiManager.getConfiguredNetworks();
		for (WifiConfiguration config : configs) {

			Log.d("xxx", config.SSID + " ?= " + ssid);
			if (config.SSID.equalsIgnoreCase("\"" + ssid + "\"")) {
				existing = config;
				exists = true;
			}
		}
		Log.d("xxx", "bssid: " + bssid + " psk: " + password + "*");
		*/
		
		//if (!exists) {
			if(isOpen){
				WifiConfiguration wifiConfig = new WifiConfiguration();
				wifiConfig.SSID = "\"" + ssid + "\"";
				wifiConfig.BSSID = bssid;
				wifiConfig.status = WifiConfiguration.Status.ENABLED;
				wifiConfig.priority = 1;
				wifiConfig.allowedKeyManagement.set(KeyMgmt.NONE);
				wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 

				mWiFiManager.setWifiEnabled(true);
				netId = mWiFiManager.addNetwork(wifiConfig);
				mWiFiManager.enableNetwork(netId, true);

				registerReceiver(broadcastReceiver, intentFilter);
				registerReceiver(br, ifil);
				receiverRegistered = true;
				new isConnected().execute();
				
			} else {
				WifiConfiguration wifiConfig = new WifiConfiguration();
				wifiConfig.SSID = "\"" + ssid + "\"";
				wifiConfig.BSSID = bssid;
				if (isWEP) {
					if (isHexWepKey(password)) {
						Log.d("xxx", "HEX = True!");
                        wifiConfig.wepKeys[0] = password;
                    } else {
                    	Log.d("xxx", "HEX = False!");
                    	wifiConfig.wepKeys[0] = "\"" + password + "\"";
                    }
					wifiConfig.wepTxKeyIndex = 0;
					wifiConfig.allowedKeyManagement.set(KeyMgmt.NONE);
					wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
					wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
					wifiConfig.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
		            wifiConfig.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
					wifiConfig.priority = 1; 
				} else
					wifiConfig.preSharedKey = "\"" + password + "\"";
				wifiConfig.status = WifiConfiguration.Status.ENABLED;

				mWiFiManager.setWifiEnabled(true);
				netId = mWiFiManager.addNetwork(wifiConfig);
				mWiFiManager.enableNetwork(netId, true);

				registerReceiver(broadcastReceiver, intentFilter);
				registerReceiver(br, ifil);
				receiverRegistered = true;

				new isConnected().execute();
			}
		//} 
		/*
		else {
			Toast.makeText(getApplicationContext(),
					"Network is already configured!", Toast.LENGTH_SHORT)
					.show();
			if(existing!=null){
				netId = mWiFiManager.updateNetwork(existing);
				mWiFiManager.enableNetwork(netId, true);
			}
		}
		*/
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
			dialog.setTitle(connecting);
			dialog.setMessage(pleasewait);
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			try {
				dialog.show();
			} catch (Exception e) {
			}
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
			try {
				dialog.dismiss();
				dialog = null;
			} catch (Exception e) {
			}
			refreshStatus();
		}
	}
	
	public void refreshStatus(){
		WifiInfo w = mWiFiManager.getConnectionInfo();
		if(w.getBSSID()!=null){
			TextView status = (TextView)findViewById(R.id.status);
			status.setText(statuscon1 + w.getSSID() + statuscon2 + w.getBSSID() + statuscon3 + w.getLinkSpeed()+"MBps");
		}
	}
	
	public void connectOrDelete(final WifiConfiguration conf){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				alreadyconfigured)
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.app_name)
				.setCancelable(true)
				.setPositiveButton(connecttt,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(conf!=null){
									netId = mWiFiManager.updateNetwork(conf);
									mWiFiManager.enableNetwork(netId, true);
									
									registerReceiver(broadcastReceiver, intentFilter);
									registerReceiver(br, ifil);
									receiverRegistered = true;
									new isConnected().execute();
								}
							}
						});
		builder.setNegativeButton(delete,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						netId = mWiFiManager.updateNetwork(conf);
						mWiFiManager.removeNetwork(netId);
						
						TextView status = (TextView)findViewById(R.id.status);
						status.setText(statusnotconnected);
						
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        
        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        
        return isHex(wepKey);
	}
	private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }
}