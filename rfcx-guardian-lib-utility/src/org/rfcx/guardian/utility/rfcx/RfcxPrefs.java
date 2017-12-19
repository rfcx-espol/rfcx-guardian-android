package org.rfcx.guardian.utility.rfcx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.rfcx.guardian.utility.FileUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class RfcxPrefs {

	public RfcxPrefs(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxPrefs.class);
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
		this.prefsDirPath = setOrCreatePrefsDirectory(context);
		
//		setDefaultPrefs();
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxPrefs.class);
	
	private Context context = null;
	private String thisAppRole = null;
	private String prefsDirPath = null;
	
	private Map<String, String> cachedPrefs = new HashMap<String, String>();
//	private Map<String, String> defaultPrefs = new HashMap<String, String>();
	
	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		if (this.cachedPrefs.containsKey(prefKey)) {
			return this.cachedPrefs.get(prefKey);
		} else if (readPrefFromFile(prefKey) != null) {
			this.cachedPrefs.put(prefKey, readPrefFromFile(prefKey));
			return this.cachedPrefs.get(prefKey);
		} else if (this.defaultPrefs.containsKey(prefKey)) {
			Log.e(logTag, "Unable to read pref '"+prefKey+"', falling back to default value '"+this.defaultPrefs.get(prefKey)+"'...");
			return this.defaultPrefs.get(prefKey);
		} else {
			return null;
		}
	}

	public int getPrefAsInt(String prefKey) {
		return (int) Integer.parseInt(getPrefAsString(prefKey));
	}

	public long getPrefAsLong(String prefKey) {
		return (long) Long.parseLong(getPrefAsString(prefKey));
	}

	public boolean getPrefAsBoolean(String prefKey) {
		return getPrefAsString(prefKey).equalsIgnoreCase("true");
	}
	
	public void setPref(String prefKey, String prefValue) {
		this.cachedPrefs.remove(prefKey);
		this.cachedPrefs.put(prefKey, prefValue);
		if ( !prefValue.equals(readPrefFromFile(prefKey)) ) {
			writePrefToFile(prefKey, prefValue);
		}
	}

	public void setPref(String prefKey, int prefValue) {
		setPref(prefKey, ""+prefValue);
	}
	
	public void clearPrefsCache() {
		this.cachedPrefs = new HashMap<String, String>();
	}
	
	// Reading and Writing to preference text files
	
	private String readPrefFromFile(String prefKey) {
		try {
    		String filePath = (new StringBuilder()).append(prefsDirPath).append("/").append(prefKey.toLowerCase(Locale.US)).append(".txt").toString();
        	File fileObj = new File(filePath);
        	
    		if (fileObj.exists()) {
				FileInputStream input = new FileInputStream(fileObj);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[256];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String txtFileContents = fileContent.toString().trim();
	    		input.close();
	    		return txtFileContents;
    		} else {
    			Log.e(logTag, "No preference file '"+prefKey+"' exists...");
    		}
    	} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
    	} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
    	return null;
	}
	
	
	private boolean writePrefToFile(String prefKey, String prefValue) {
		
		boolean writeSuccess = false;
		
    	String filePath = prefsDirPath+"/"+prefKey.toLowerCase(Locale.US)+".txt";
    	File fileObj = new File(filePath);
    	
    	if (!fileObj.exists()) {
        	(new File(prefsDirPath)).mkdirs();
        	FileUtils.chmod(prefsDirPath, 0777);
    	} else {
    		fileObj.delete();
    	}
    	
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
        	outFile.write(prefValue);
        	outFile.close();
        	FileUtils.chmod(filePath, 0777);
        	writeSuccess = fileObj.exists();
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
        return writeSuccess;
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianRoleTxtFile(this.context, "version",versionName);
	}
	
	public String getVersionFromFile(String targetAppRole) {
		return readFromGuardianRoleTxtFile(this.context, this.thisAppRole, targetAppRole.toLowerCase(Locale.US), "version");
	}

	private void writeToGuardianRoleTxtFile(Context context, String fileNameNoExt, String stringContents) {
    	String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt";
    	File fileObj = new File(filePath);
    	fileObj.mkdirs();
    	FileUtils.chmod(context.getFilesDir().toString()+"/txt", 0755);
    	if (fileObj.exists()) { fileObj.delete(); }
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
        	outFile.write(stringContents);
        	outFile.close();
        	FileUtils.chmod(filePath, 0755);
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
	}
	
	private String readFromGuardianRoleTxtFile(Context context, String thisAppRole, String targetAppRole, String fileNameNoExt) {
    	try {
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		File txtFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+thisAppRole).length()))+"."+targetAppRole+"/files/txt",fileNameNoExt+".txt");
    		if (txtFile.exists()) {
				FileInputStream input = new FileInputStream(txtFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[256];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String txtFileContents = fileContent.toString().trim();
	    		input.close();
	    		return txtFileContents;
    		} else {
    			Log.e(logTag, "No file '"+fileNameNoExt+"' saved by org.rfcx.guardian."+targetAppRole+"...");
    		}
    	} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
    	} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
    	return null;
	}

	
	// this may require root...
	private static String setOrCreatePrefsDirectory(Context context) {
		
		String prefsDir = (new StringBuilder()).append(context.getFilesDir().toString()).append("/prefs").toString();
		(new File(prefsDir)).mkdirs(); FileUtils.chmod(prefsDir, 0777);

//		String sdCardDir = (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/prefs").toString();
//		if (!(new File(sdCardDir)).isDirectory()) { (new File(sdCardDir)).mkdirs(); FileUtils.chmod(sdCardDir, 0777); }
		
		return prefsDir;
		
	}
	
	public static List<String> listPrefsKeys() {
		List<String> prefsKeys = new ArrayList<String>();
		for (Entry prefKeyEntry : defaultPrefs.entrySet()) {
			prefsKeys.add(prefKeyEntry.getKey().toString());
		}
		return prefsKeys;
	}
	
	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
	    new HashMap<String, String>() {{

			put("checkin_offline_mode", "false");
			
	        put("api_url_base", "https://api.rfcx.org");
	        
	        put("service_monitor_cycle_duration", "600000");
	        
			put("reboot_forced_daily_at", "23:55:00");

			put("install_api_registration_token", "");
			put("install_cycle_duration", "3600000");
			put("install_offline_toggle_threshold", "900000");
			
			put("cputuner_freq_min", "30720");
			put("cputuner_freq_max", "122880"); // options: 30720, 49152, 61440, 122880, 245760, 320000, 480000, 
			put("cputuner_governor_up", "98");
			put("cputuner_governor_down", "95");
			
			put("audio_cycle_duration", "90000");
			put("audio_schedule_off_hours", "00:10-00:15,00:15-00:20");
			
			put("audio_encode_codec", "opus");
			put("audio_encode_bitrate", "16384");
			//@arrglado
			put("audio_sample_rate", "12000");
			//

			put("audio_encode_quality", "9");
			put("audio_encode_skip_threshold", "3");
			put("audio_encode_cycle_pause", "5000");

			//@arreglado
			put("checkin_battery_cutoff", "90");
			put("audio_battery_cutoff", "60");
			//

			put("install_battery_cutoff", "30");
			
			put("checkin_cycle_pause", "5000");
			put("checkin_skip_threshold", "5");
			put("checkin_stash_threshold", "120");
			put("checkin_archive_threshold", "960");
			
	    }}
	);

	
//	private void setDefaultPrefs() {
//		
//		defaultPrefs.put("checkin_offline_mode", "false");
//	
//		defaultPrefs.put("api_url_base", "https://api.rfcx.org");
//
//		defaultPrefs.put("service_monitor_cycle_duration", "600000");
//
//		defaultPrefs.put("reboot_forced_daily_at", "23:55:00");
//		
//		defaultPrefs.put("install_cycle_duration", "3600000");
//		defaultPrefs.put("install_offline_toggle_threshold", "900000");
//		defaultPrefs.put("install_api_registration_token", "ABCDEFGH");
//		
//		defaultPrefs.put("cputuner_freq_min", "30720");
//		defaultPrefs.put("cputuner_freq_max", "245760"); // options: 30720, 49152, 61440, 122880, 245760, 320000, 480000, 
//		defaultPrefs.put("cputuner_governor_up", "98");
//		defaultPrefs.put("cputuner_governor_down", "95");
//		
//		defaultPrefs.put("audio_cycle_duration", "90000");
//		defaultPrefs.put("audio_schedule_off_hours", "00:10-00:15,00:15-00:20");
//		
//		defaultPrefs.put("audio_encode_codec", "opus");
//		defaultPrefs.put("audio_encode_bitrate", "16384");
//		defaultPrefs.put("audio_sample_rate", "12000");
//		defaultPrefs.put("audio_encode_quality", "9");
//		defaultPrefs.put("audio_encode_skip_threshold", "3");
//		defaultPrefs.put("audio_encode_cycle_pause", "5000");
//
//		defaultPrefs.put("checkin_battery_cutoff", "90");
//		defaultPrefs.put("audio_battery_cutoff", "60");
//		defaultPrefs.put("install_battery_cutoff", "30");
//		
//		defaultPrefs.put("checkin_cycle_pause", "5000");
//		defaultPrefs.put("checkin_skip_threshold", "5");
//		defaultPrefs.put("checkin_stash_threshold", "320");
//		defaultPrefs.put("checkin_archive_threshold", "960");
//		
//	}
	
}
