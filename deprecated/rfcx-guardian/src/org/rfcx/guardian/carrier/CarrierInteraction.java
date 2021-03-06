package org.rfcx.guardian.carrier;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CarrierInteraction {

	private static final String TAG = "RfcxGuardian-"+CarrierInteraction.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	private static final String HASH = Uri.encode("#");
	
	public String currentlyRunningCode = null;
	
	public void submitCode(Context context, String code) {
        try {
        	Intent callIntent = new Intent("android.intent.action.CALL",Uri.parse("tel:"+code.replaceAll("#", HASH)));
        	callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	context.startActivity(callIntent);
        } catch (Exception e) {
        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
        }	
	}
	
	public void closeResponseDialog(Context context, String[] commandSequence) {
		List<String> cmdSeq = new ArrayList<String>();
		for (String command : commandSequence) {
			cmdSeq.add("input keyevent "+command.replaceAll("up","19").replaceAll("down","20").replaceAll("right","22").replaceAll("left","21").replaceAll("enter","23"));
		}
		
		// add a couple of "enter" presses in case some extra popups come up (it definitely happens sometimes)
		cmdSeq.add("input keyevent 23");
		cmdSeq.add("input keyevent 23");
		
		Log.d(TAG, TextUtils.join(" && ", cmdSeq));
		(new ShellCommands()).executeCommandAsRoot(TextUtils.join(" && ", cmdSeq),null,context);
	}	
}
