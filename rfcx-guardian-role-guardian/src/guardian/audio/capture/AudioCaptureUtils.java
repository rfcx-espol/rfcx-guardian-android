package guardian.audio.capture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.text.TextUtils;
import guardian.RfcxGuardian;

public class AudioCaptureUtils {

	public AudioCaptureUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		RfcxAudioUtils.initializeAudioDirectories(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioCaptureUtils.class);

	private RfcxGuardian app = null;
	
	public long[] captureTimeStampQueue = new long[] { 0, 0 };

	public boolean updateCaptureTimeStampQueue(long timeStamp) {
		captureTimeStampQueue[0] = captureTimeStampQueue[1];
		captureTimeStampQueue[1] = timeStamp;
		return (captureTimeStampQueue[0] > 0);
	}
	
	public boolean isBatteryChargeSufficientForCapture() {
		//@modificado
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff"));
	}

	public boolean isCaptureAllowedAtThisTimeOfDay() {
		for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours"), ",")) {
			String[] offHours = TextUtils.split(offHoursRange, "-");
			if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
				return false;
			}
		}
		return true;
	}
	
	public static String getCaptureFilePath(String captureDir, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(captureDir).append("/").append(timestamp).append(".").append(fileExtension).toString();
	}
	
	public static AudioCaptureWavRecorder getWavRecorder(String captureDir, long timestamp, String fileExtension, int sampleRate) throws IllegalStateException, IOException, IllegalThreadStateException {
		AudioCaptureWavRecorder rec = null;
		rec = AudioCaptureWavRecorder.getInstance(sampleRate);
		rec.setOutputFile(getCaptureFilePath(captureDir,timestamp,fileExtension));
		rec.prepare();
        return rec;
	}
	
	public static boolean reLocateAudioCaptureFile(Context context, long timestamp, String fileExtension) {
		boolean isFileMoved = false;
		File captureFile = new File(getCaptureFilePath(RfcxAudioUtils.captureDir(context),timestamp,fileExtension));
		if (captureFile.exists()) {
			try {
				File preEncodeFile = new File(RfcxAudioUtils.getAudioFileLocation_PreEncode(context, timestamp,fileExtension));
				FileUtils.copy(captureFile, preEncodeFile);
				FileUtils.chmod(preEncodeFile, 0777);
				if (preEncodeFile.exists()) { captureFile.delete(); }	
				isFileMoved = preEncodeFile.exists();
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return isFileMoved;
	}

}
