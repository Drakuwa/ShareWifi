package com.app.sharewifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;

/**
 * A model class that resolves some of the business logic in the application.
 * Also as a part of the MVC(Model View Controller) programming pattern. It
 * presents changes to the UI thread through the Controller classes.
 * 
 * @author drakuwa
 */
public class Model {

	private Context ctx;
	
	private Resources res;
	private String welcometo;
	private String ok;
	private String internetoptions;
	private String internetconnectiondisabled;
	private String unsentbugs;

	/**
	 * Constructor of the Model class which initializes the activity context.
	 * 
	 * @param context
	 */
	public Model(Context context) {
		this.ctx = context;
		res = ctx.getResources();
		welcometo = res.getString(R.string.welcometo);
		ok = res.getString(R.string.ok);
		internetoptions = res.getString(R.string.internetoptions);
		internetconnectiondisabled = res.getString(R.string.internetconnectiondisabled);
		unsentbugs = res.getString(R.string.unsentbugs);
	}

	/**
	 * Method that checks if the application is run for the first time. It
	 * checks for the existence of an empty file in the application folder, and
	 * if it doesn't it creates an AlertDialog with the welcome message, and it
	 * creates the file.
	 */
	public void first_run() {
		boolean exists = (new File("/data/data/com.app.busmk2/notwelcomefirst"))
				.exists();

		if (!exists) {
			// Welcome note...
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(
					welcometo).setIcon(
					R.drawable.ic_launcher).setTitle(R.string.app_name).setCancelable(
					false).setPositiveButton(ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
			try {
				new File("/data/data/com.app.busmk2/notwelcomefirst")
						.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Method that creates and shows an AlertDialog with a message passed with
	 * the txt parameter, and a PositiveButton "OK..."
	 * 
	 * @param txt
	 */
	public void CustomDialog(final String txt) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(txt).setIcon(R.drawable.ic_launcher).setTitle(
				R.string.app_name).setCancelable(false).setPositiveButton(
				ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alert = builder.create();
		try {
			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create an alert dialog that redirects you to the internet options on the
	 * phone, so you can enable an internet connection
	 */
	public void createInternetDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder
				.setMessage(
						internetconnectiondisabled)
				.setIcon(R.drawable.ic_launcher).setTitle(R.string.app_name)
				.setCancelable(false).setPositiveButton(internetoptions,
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
		ctx.startActivity(netOptionsIntent);
	}
	
	/**
	 * A function that checks the existence of stack.trace file and calls a
	 * function alert for sending/deleting it
	 */
	public void checkBugs() {
		File file = new File("/data/data/com.app.sharewifi/files/stack.trace");
		if (file.exists()) {
			String line = "";
			String trace = "";
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								ctx
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
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(
				unsentbugs)
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

								ctx.startActivity(Intent
										.createChooser(sendIntent, "Title:"));

								ctx
										.deleteFile("stack.trace");
							}
						});
		builder.setNegativeButton("Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						File file = new File(
								"/data/data/com.app.sharewifi/files/stack.trace");
						if (file.exists()) {
							ctx
									.deleteFile("stack.trace");
						}
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public class MapComparator implements Comparator<Map<String, ?>> {
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