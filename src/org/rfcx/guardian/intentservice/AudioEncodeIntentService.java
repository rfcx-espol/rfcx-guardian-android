package org.rfcx.guardian.intentservice;

import java.io.File;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AudioEncodeIntentService extends IntentService {
	
	private static final String TAG = AudioEncodeIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.AUDIO_ENCODE";
	public static final String NULL_EXC = "org.rfcx.guardian.RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";
	
	public AudioEncodeIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NULL_EXC);
		
		List<String[]> capturedRows = app.audioDb.dbCaptured.getAllCaptured();
		if (app.verboseLog) { Log.d(TAG, "Running AudioEncodeIntentService... "+capturedRows.size()+" files to encode."); }
		for (String[] capturedRow : capturedRows) {
			if (app.verboseLog) { Log.d(TAG, "Encoding: '"+capturedRow[0]+"','"+capturedRow[1]+"','"+capturedRow[2]+"'"); }
			if (capturedRow[2].equals("wav")) {
				app.audioCore.encodeCaptureAudio(capturedRow[1], "flac", capturedRow[0], app.audioDb);
			} else {
				Log.d(TAG, "Captured file does not need to be re-encoded...");
				app.audioDb.dbCaptured.clearCapturedBefore(app.audioDb.dateTimeUtils.getDateFromString(capturedRow[0]));
				String digest = (new FileUtils()).sha1Hash(app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+capturedRow[2]+"/"+capturedRow[1]+"."+capturedRow[2]);
				app.audioDb.dbEncoded.insert(capturedRow[1], capturedRow[2],digest);
			}
		}
		
		// Trigger CheckIn ???
		app.apiCore.sendCheckIn(app);
	}

}