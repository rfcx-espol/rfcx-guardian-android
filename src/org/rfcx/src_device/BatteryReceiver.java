package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

	private static final String TAG = BatteryReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
        setBatteryState(context, intent);
        allowOrDisAllowServices(context, intent);
	}
	
	private void setBatteryState(Context context, Intent intent) {
		RfcxSource rfcxSource = (RfcxSource) context.getApplicationContext();
		rfcxSource.deviceState.setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		rfcxSource.deviceState.setBatteryScale(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
		rfcxSource.deviceState.setBatteryTemperature(Math.round(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)/10));
		rfcxSource.deviceStateDb.dbBattery.insert(rfcxSource.deviceState.getBatteryPercent());
	}
	
	private void allowOrDisAllowServices(Context context, Intent intent) {
		DeviceState deviceState = ((RfcxSource) context.getApplicationContext()).deviceState;
		if (deviceState.allowServices()) {
			Log.d(TAG, "Battery percentage: "+deviceState.getBatteryPercent()+"%, services SHOULD be allowed to run...");
		} else {
			Log.d(TAG, "Battery percentage: "+deviceState.getBatteryPercent()+"%, services should NOT be allowed to run...");
		}
	}
	
}