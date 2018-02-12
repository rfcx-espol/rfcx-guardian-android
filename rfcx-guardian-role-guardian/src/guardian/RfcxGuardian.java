package guardian;

import java.util.Map;

import org.rfcx.guardian.guardian.R;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.device.control.DeviceControlUtils;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceCPU;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.DeviceNetworkStats;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;
import guardian.api.checkin.ApiCheckInDb;
import guardian.api.checkin.ApiCheckInJobService;
import guardian.api.checkin.ApiCheckInLoopService;
import guardian.api.checkin.ApiCheckInQueueIntentService;
import guardian.api.checkin.ApiCheckInUtils;
import guardian.audio.capture.AudioCaptureService;
import guardian.audio.capture.AudioCaptureUtils;
import guardian.audio.encode.AudioEncodeDb;
import guardian.audio.encode.AudioEncodeJobService;
import guardian.audio.encode.AudioEncodeLoopService;
import guardian.audio.encode.AudioEncodeQueueIntentService;
import guardian.device.android.DeviceDataTransferDb;
import guardian.device.android.DeviceLogCatCaptureDb;
import guardian.device.android.DeviceRebootDb;
import guardian.device.android.DeviceScreenShotDb;
import guardian.device.android.DeviceSensorDb;
import guardian.device.android.DeviceSystemDb;
import guardian.device.android.DeviceSystemService;
import guardian.receiver.ConnectivityReceiver;
import guardian.receiver.SmsReceiver;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

	public String version;
	Context context;
	
	public static final String APP_ROLE = "Guardian";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);
	
	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;

	public SharedPreferences sharedPrefs = null;
	
	// Database Handlers
	public AudioEncodeDb audioEncodeDb = null;
	public ApiCheckInDb apiCheckInDb = null;
	public DeviceSystemDb deviceSystemDb = null;
	public DeviceSensorDb deviceSensorDb = null;
	public DeviceRebootDb rebootDb = null;
	public DeviceDataTransferDb deviceDataTransferDb = null;
	public DeviceScreenShotDb deviceScreenShotDb = null;
	public DeviceLogCatCaptureDb deviceLogCatCaptureDb = null;
	
	// Receivers
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	
	// Android Device Handlers
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
	public DeviceNetworkStats deviceNetworkStats = new DeviceNetworkStats(APP_ROLE);
	public DeviceCPU deviceCPU = new DeviceCPU(APP_ROLE);

	// Misc
	public AudioCaptureUtils audioCaptureUtils = null;
	public ApiCheckInUtils apiCheckInUtils = null;
	
	public DeviceControlUtils deviceControlUtils = new DeviceControlUtils(APP_ROLE);
	public DeviceBluetooth deviceBluetooth = new DeviceBluetooth(APP_ROLE);
	
	public String[] RfcxCoreServices = 
		new String[] { 
			"AudioCapture",
			"AudioEncodeLoop",
			//"ApiCheckInLoop",
			"DeviceSystem"
		};

	//public static boolean TERMINATE_ENCODING = true;
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, logTag);
		this.rfcxPrefs.writeVersionToFile(this.version);
		
		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		this.syncSharedPrefs();
		
		setDbHandlers();
		setServiceHandlers();
		
		this.audioCaptureUtils = new AudioCaptureUtils(getApplicationContext());
		this.apiCheckInUtils = new ApiCheckInUtils(getApplicationContext());
		
		initializeRoleServices();
	}
	
	public void onTerminate() {
		super.onTerminate();
		this.unregisterReceiver(connectivityReceiver);
	}
	
	public void appResume() {
		syncSharedPrefs();
	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices() {
 		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			
			String[] onLaunchServices = new String[RfcxCoreServices.length+1];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			onLaunchServices[RfcxCoreServices.length] = 
					"ServiceMonitor"
						+"|"+DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
						+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
						;
			
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true);
		}
	}
	
	private void setDbHandlers() {
		
		this.audioEncodeDb = new AudioEncodeDb(this, this.version);
		this.apiCheckInDb = new ApiCheckInDb(this, this.version);
		this.deviceSystemDb = new DeviceSystemDb(this, this.version);
		this.deviceSensorDb = new DeviceSensorDb(this, this.version);
		this.rebootDb = new DeviceRebootDb(this, this.version);
		this.deviceDataTransferDb = new DeviceDataTransferDb(this, this.version);
		this.deviceScreenShotDb = new DeviceScreenShotDb(this, this.version);
		this.deviceLogCatCaptureDb = new DeviceLogCatCaptureDb(this, this.version);

	}
	
	private void setServiceHandlers() {

		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
		this.rfcxServiceHandler.addService("AudioCapture", AudioCaptureService.class);
		this.rfcxServiceHandler.addService("AudioEncodeQueue", AudioEncodeQueueIntentService.class);
		this.rfcxServiceHandler.addService("AudioEncodeLoop", AudioEncodeLoopService.class);
		this.rfcxServiceHandler.addService("AudioEncodeJob", AudioEncodeJobService.class);
		this.rfcxServiceHandler.addService("DeviceSystem", DeviceSystemService.class);
		this.rfcxServiceHandler.addService("ApiCheckInQueue", ApiCheckInQueueIntentService.class);
		this.rfcxServiceHandler.addService("ApiCheckInLoop", ApiCheckInLoopService.class);
		this.rfcxServiceHandler.addService("ApiCheckInJob", ApiCheckInJobService.class);
		
	}
	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
		Log.d(logTag, "Pref changed: "+prefKey+" = "+this.sharedPrefs.getString(prefKey, null));
		syncSharedPrefs();
	}
	
	private void syncSharedPrefs() {
		for ( Map.Entry<String,?> pref : this.sharedPrefs.getAll().entrySet() ) {
			this.rfcxPrefs.setPref(pref.getKey(), pref.getValue().toString());
		}
	}
	
	public boolean setPref(String prefKey, String prefValue) {
		return this.sharedPrefs.edit().putString(prefKey,prefValue).commit();
	}
    
}
