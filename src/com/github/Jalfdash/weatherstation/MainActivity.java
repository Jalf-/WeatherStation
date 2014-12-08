package com.github.Jalfdash.weatherstation;

import java.io.IOException;
import java.util.Timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{
	static String TAG;
	protected static String KEY = "42";
	
	Bluetooth bluetooth;
	static String bluetoothData;
	String[] bluetoothDataSplit;
	
	private Timer timer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TAG = getString(R.string.app_name);
		
		// Bluetooth
		bluetooth = new Bluetooth(this);
		bluetooth.checkBt();

		if (bluetooth.connect())
		{
			// Start timer and repeat task every second.
			Log.d(TAG, "Starting timer loop.");
			timer = new Timer();
			UpdateCycle updateCycle = new UpdateCycle(this);
			timer.schedule(updateCycle, 500, 5000);	
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		// Close Bluetooth connection when app is closed.
		try
		{
			bluetooth.getBtSocket().close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if (timer != null) timer.cancel();
	}
	
//	http://stackoverflow.com/questions/2663491/close-application-on-error
	
	public void closeApplication(String e)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setMessage("There was an error: " + e)
            .setCancelable(false)
            .setNeutralButton("Ok.", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                	MainActivity.this.finish();
                }
            });
            AlertDialog error = builder.create();
            error.show();
            return;
	}
}
