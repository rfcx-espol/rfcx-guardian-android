package guardian.audio.encode;

import java.io.File;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;
import guardian.audio.encode.AudioEncodeUtils;

public class AudioEncodeJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioEncodeJobService.class);
	
	private static final String SERVICE_NAME = "AudioEncodeJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioEncodeJob audioEncodeJob;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncodeJob = new AudioEncodeJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioEncodeJob.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioEncodeJob.interrupt();
		this.audioEncodeJob = null;
	}
	
	private class AudioEncodeJob extends Thread {

		public AudioEncodeJob() {
			super("AudioEncodeJobService-AudioEncodeJob");
		}
		
		@Override
		public void run() {
			AudioEncodeJobService audioEncodeJobInstance = AudioEncodeJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			int prefsEncodeSkipThreshold = app.rfcxPrefs.getPrefAsInt("audio_encode_skip_threshold");
			int prefsAudioEncodeCyclePause = app.rfcxPrefs.getPrefAsInt("audio_encode_cycle_pause");
			int prefsAudioBatteryCutoff = app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff");
			long prefsCaptureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int prefsAudioEncodeQuality = app.rfcxPrefs.getPrefAsInt("audio_encode_quality");
			
			AudioEncodeUtils.cleanupEncodeDirectory(context, app.audioEncodeDb.dbEncodeQueue.getAllRows());
			
			while (audioEncodeJobInstance.runFlag) {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
				try {
					
					String[] audioToEncode = app.audioEncodeDb.dbEncodeQueue.getLatestRow();
										
					// only proceed with encode process if:
					if (	// 1) there is a queued audio file in the database
							(audioToEncode[0] != null)
							// 2) the device internal battery percentage is at or above the minimum charge threshold
								&&	(app.deviceBattery.getBatteryChargePercentage(context,null) >= prefsAudioBatteryCutoff/*0*/)
						) {
						
							File preEncodeFile = new File(audioToEncode[9]);
							
							if (!preEncodeFile.exists()) {
								
								app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(audioToEncode[1]);
								
							} else if (((int) Integer.parseInt(audioToEncode[10])) >= prefsEncodeSkipThreshold) {
								
								Log.d(logTag, (new StringBuilder()).append("Skipping AudioEncodeJob ").append(audioToEncode[1]).append(" after ").append(prefsEncodeSkipThreshold).append(" failed attempts").toString());
								
								app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(audioToEncode[1]);
								if (preEncodeFile.exists()) { preEncodeFile.delete(); }
								
							} else {
								
								try {
									
									Log.i(logTag, (new StringBuilder()).append("Beginning Encode: ").append(audioToEncode[1]).append(" ").append(audioToEncode[2]).append("=>").append(audioToEncode[6]).toString());
								
									File postEncodeFile = new File(RfcxAudioUtils.getAudioFileLocation_PostEncode(context, (long) Long.parseLong(audioToEncode[1]),audioToEncode[6]));
									File gZippedFile = new File(RfcxAudioUtils.getAudioFileLocation_Complete_PostZip(context, (long) Long.parseLong(audioToEncode[1]),RfcxAudioUtils.getFileExtension(audioToEncode[6])));

									// just in case there's already a post-encoded file, delete it first
									if (postEncodeFile.exists()) { postEncodeFile.delete(); }

									long encodeStartTime = System.currentTimeMillis();

									// perform audio encoding and set encoding eventual bit rate
									int encodeBitRate = 
										AudioEncodeUtils.encodeAudioFile(
											preEncodeFile, 								// source file
											postEncodeFile, 							// target file
											audioToEncode[6], 							// encoding codec
											(int) Integer.parseInt(audioToEncode[4]), 	// encoding sample rate
											(int) Integer.parseInt(audioToEncode[5]), 	// encoding target bitrate
											prefsAudioEncodeQuality						// encoding quality
										);

									long encodeDuration = (System.currentTimeMillis() - encodeStartTime);

									if (encodeBitRate < 0) {

										app.audioEncodeDb.dbEncodeQueue.incrementSingleRowAttempts(audioToEncode[1]);
										
									} else {

										// delete pre-encode file
										if (preEncodeFile.exists() && postEncodeFile.exists()) { preEncodeFile.delete(); }

										// generate file checksum of encoded file
										String preZipDigest = FileUtils.sha1Hash(postEncodeFile.getAbsolutePath());

										// GZIP encoded file into final location
										GZipUtils.gZipFile(postEncodeFile, gZippedFile);

										// If successful, cleanup pre-GZIP file and make sure final file is accessible by other roles (like 'api')
										if (gZippedFile.exists()) {

											FileUtils.chmod(gZippedFile, 0777);
											if (postEncodeFile.exists()) { postEncodeFile.delete(); }

											app.audioEncodeDb.dbEncoded
												.insert(
													audioToEncode[1], 
													RfcxAudioUtils.getFileExtension(audioToEncode[6]), 
													preZipDigest, 
													(int) Integer.parseInt(audioToEncode[4]), 
													encodeBitRate, 
													audioToEncode[6], 
													(long) Long.parseLong(audioToEncode[7]),
													encodeDuration, 
													gZippedFile.getAbsolutePath()
												);

											app.audioEncodeDb.dbEncodeQueue.deleteSingleRow(audioToEncode[1]);

											app.rfcxServiceHandler.triggerIntentServiceImmediately("ApiCheckInQueue");
										}
										
									}
									
								} catch (Exception e) {
									RfcxLog.logExc(logTag, e);
									app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
									audioEncodeJobInstance.runFlag = false;
								}
							}
					} else {

						// force a [brief] pause before re-running each cycle
						Thread.sleep(prefsAudioEncodeCyclePause);
						
						if (app.deviceBattery.getBatteryChargePercentage(context,null) < prefsAudioBatteryCutoff) {
							long extendEncodeLoopBy = (2 * prefsCaptureLoopPeriod) - prefsAudioEncodeCyclePause;
							Log.i(logTag, (new StringBuilder())
									.append("AudioEncodeJob disabled due to low battery level (")
									.append("current: ").append(app.deviceBattery.getBatteryChargePercentage(context, null)).append("%, ")
									.append("required: ").append(prefsAudioBatteryCutoff).append("%")
									.append("). Waiting ").append((Math.round(2*prefsCaptureLoopPeriod/1000))).append(" seconds before next attempt.")
									.toString()
									);
							Thread.sleep(extendEncodeLoopBy);
						}
					}
						
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					audioEncodeJobInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioEncodeJobInstance.runFlag = false;

		}
	}
	

}
