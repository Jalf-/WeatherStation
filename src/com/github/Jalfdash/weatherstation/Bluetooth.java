package com.github.Jalfdash.weatherstation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * Bluetooth class that handles the Bluetooth connection.
 * Bluetooth source code 
 * http://forum.arduino.cc/index.php?topic=157621.0
 * With own additions. 
 */
public class Bluetooth
{
	static String bluetoothData = "0, 0";
	
	private MainActivity mainActivity;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	private InputStream inStream = null;
	private boolean stopWorker = false;
	private byte delimiter = 10;
	private int readBufferPosition = 0;
	private byte[] readBuffer = new byte[1024];
	private Handler handler = new Handler();
	private static String address = "00:00:00:00:00:00";

	public Bluetooth(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;
	}
	
	public void checkBt()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
		
		if (bondedDevices.isEmpty())
		{
			Log.e(MainActivity.TAG, "No devices paired...");
			return;
		}
		
		for (BluetoothDevice device : bondedDevices)
		{
			if (device.getName().equalsIgnoreCase("Benjabi"))
			{
				Log.d(MainActivity.TAG, "Device: address: " + device.getAddress() + " name: " + device.getName());
				address = device.getAddress();
				break;
			}
		}
	}
	
	public boolean connect()
	{	
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		Log.d(MainActivity.TAG, "Connecting to ... " + address);
		mBluetoothAdapter.cancelDiscovery();
		UUID MY_UUID = UUID
	            .fromString("00001101-0000-1000-8000-00805F9B34FB");
		try
		{
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			btSocket.connect();
			Log.d(MainActivity.TAG, "Connection made.");
		}
		catch (IOException e)
		{
			try
			{
				btSocket.close();
			}
			catch (IOException e2)
			{
				Log.d(MainActivity.TAG, "Unable to end the connection.");
			}
			Log.d(MainActivity.TAG, "Socket creation failed.");
			mainActivity.closeApplication(e.getMessage());
			return false;
		}
		beginListenForData();
		return true;
	}

	private void beginListenForData()
	{
		try
		{
			inStream = btSocket.getInputStream();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		Thread workerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(!Thread.currentThread().isInterrupted() && !stopWorker )
				{
					try
					{
						int bytesAvaliable = inStream.available();

						if (bytesAvaliable > 0)
						{
							byte[] packetsBytes = new byte[bytesAvaliable];
							inStream.read(packetsBytes);
							for(int i = 0; i < bytesAvaliable; i++)
							{
								byte b = packetsBytes[i];
								if (b == delimiter)
								{
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;
									handler.post(new Runnable()
									{	
										@Override
										public void run()
										{
											setBluetoothData(data);
										}
									});
								}
								else
								{
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					}
					catch(IOException ex)
					{
						stopWorker = true;
					} 
					catch(NullPointerException ex)
					{
						stopWorker = true;
					} 
				}
			}
		});	
		workerThread.start();		
	}
	
	/**
	 * @return Bluetooth socket.
	 */
	public BluetoothSocket getBtSocket()
	{
		return btSocket;
	}

	public String getBluetoothData()
	{
		return bluetoothData;
	}

	public void setBluetoothData(String bluetoothData)
	{
		Bluetooth.bluetoothData = bluetoothData;
	}
}
