package org.rfcx.guardian.api;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.TimeOfDay;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ApiConnectIntentService extends IntentService {

	private static final String TAG = ApiConnectIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.API_CONNECT";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_API_CONNECT_NOTIFICATIONS";
	
	public ApiConnectIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		TimeOfDay timeOfDay = new TimeOfDay();
		if (app.isCrisisModeEnabled) {
			if (app.verboseLogging) Log.d(TAG, "Crisis mode enabled! Not contacting API...");
			app.airplaneMode.setOn(context);	
		} else if (app.isServiceRunning_ApiComm && timeOfDay.isDataGenerationEnabled(context)) {
			app.airplaneMode.setOff(context);	
			Intent intent = new Intent(INTENT_TAG);
			sendBroadcast(intent, NOTIFICATION_TAG);
			ApiCore apiCore = new ApiCore();
			if (apiCore.getConnectivityTimeout() > 0) {
				try {
					Thread.sleep(apiCore.getConnectivityTimeout()*1000);
					if (!app.airplaneMode.isEnabled(context)) {
						if (!app.apiCore.isTransmitting) {
							Log.d(TAG, "Connectivity timeout reached. Entering Airplane Mode.");
							apiCore.resetTransmissionState();
							app.airplaneMode.setOn(context);
						} else {
							Log.d(TAG, "Connectivity timeout reached, but transmission is in progress. Delaying timeout.");
							Thread.sleep(60*1000);
							if (!app.airplaneMode.isEnabled(context)) {
								Log.d(TAG, "2nd timeout reached. Entering Airplane Mode.");
								apiCore.resetTransmissionState();
								app.airplaneMode.setOn(context);
							}
						}
					}
				} catch (InterruptedException e) {
				}
			}
		} else {
			if (app.verboseLogging) Log.d(TAG, app.isServiceRunning_ApiComm ? "Skipping (off hours)" : "Skipping (first run)");
			if (timeOfDay.isDataGenerationEnabled(context)) {
				app.airplaneMode.setOn(context);
			}
			app.isServiceRunning_ApiComm = true;
		}
	}
}
