package guardian.receiver;

import java.util.Calendar;

import org.rfcx.guardian.utility.device.control.DeviceAirplaneMode;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import guardian.RfcxGuardian;

public class AirplaneModeReceiver extends BroadcastReceiver {
	
	private String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
//		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		Log.v(logTag, "AirplaneMode " + ( DeviceAirplaneMode.isEnabled(context) ? "Enabled" : "Disabled" )
						+ " at "+(Calendar.getInstance()).getTime().toLocaleString());
		
	}

}
