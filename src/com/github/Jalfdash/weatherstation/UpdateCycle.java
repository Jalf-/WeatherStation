package com.github.Jalfdash.weatherstation;

import java.text.DecimalFormat;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateCycle extends TimerTask
{
	Bluetooth bluetooth;
	String[] bluetoothDataSplit;
	
	DoHttpPost doHttpPost;
	
	MainActivity mainActivity;
	
	TextView windSpeedText;
	ImageView image;
	Bitmap bMap;
	Matrix mat;
	
	public UpdateCycle(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;
		bluetooth = new Bluetooth(mainActivity);
		bluetoothDataSplit = new String[2];
		
		mainActivity.setContentView(R.layout.activity_main);
		windSpeedText = (TextView) mainActivity.findViewById(R.id.windSpeedTextView);
		image = (ImageView) mainActivity.findViewById(R.id.imageView1);
		bMap = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.arrow);
		mat = new Matrix();
	}
	
	@Override
	public void run()
	{
		if (bluetooth.getBluetoothData().contains("Failed to read from sensors!")) return;
		
		// Get input data.
		Log.d(MainActivity.TAG, "update: " + bluetooth.getBluetoothData());
		
		bluetoothDataSplit = bluetooth.getBluetoothData().split(",");
		
		// Wind speed is measured in meters per second.
		final double windSpeed = calculateWindSpeed(Integer.parseInt(bluetoothDataSplit[0]));
		
		int windDirection = calculateWindDirection(bluetoothDataSplit[1]);
		
		mat.setRotate(windDirection);
		final Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), mat, true);
		
		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run() {
				windSpeedText.setText(mainActivity.getString(R.string.wind_speed) + " " + windSpeed + " m/s");
				image.setImageBitmap(bMapRotate);
			}
		});
		
		String[] dataString = {String.valueOf(windSpeed), String.valueOf(windDirection)};
		
		Log.d(MainActivity.TAG, "" + dataString[0] + " " + dataString[1]);
		sendData(dataString);
	}
	
	public double calculateWindSpeed(int rotation)
	{	
		double speed = 0.0183 * rotation + 1.9071;
		
		if (rotation == 0) speed = 0;
		
		DecimalFormat df = new DecimalFormat("#.#");
		return Double.parseDouble(df.format(speed).replace(",", "."));
	}
	
	public int calculateWindDirection(String bitValues)
	{		
		String[] bitRefTable = {"10000", "10001", "10101", "11101", "11001", "01001", "00001", "00011",
				"01011", "11011", "10011", "10010", "00010", "00110", "10110", "00111",
				"00101", "00100", "01100", "01101", "01111", "01110", 
				"01010", "01000", "11000", "11010", "11110", "11100", "10100"};
		
		int[] directionTable = {0, 349, 338, 326, 315, 304, 293, 281, 270,
				255, 240, 225, 210, 195, 180,
				169, 158, 146, 135, 124, 113, 101, 90,
				77, 64, 52, 39, 26, 13};
		
		for (int i = 0; i < bitRefTable.length; i++)
		{
			if (bitValues.equalsIgnoreCase(bitRefTable[i]))
			{
				return directionTable[i];
			}
		}

		return 0;
	}
	
	public void sendData(String[] data)
	{
		ConnectivityManager connManager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.isConnected())
			new DoHttpPost().execute(data[0], data[1]);
		else 
			Toast.makeText(mainActivity.getApplicationContext(), "Couldn't connect to website!", Toast.LENGTH_LONG).show();
	}
}
