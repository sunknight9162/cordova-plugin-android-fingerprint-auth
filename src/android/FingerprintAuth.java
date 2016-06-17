package com.cordova.plugin.android.fingerprintauth;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.util.Log;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(23)
public class FingerprintAuth extends CordovaPlugin {

	public static final String TAG = "FingerprintAuth";
	public static String packageName;

	private static final String DIALOG_FRAGMENT_TAG = "FpAuthDialog";

	FingerprintAuthenticationDialogFragment mFragment;

	private FingerprintManager mFingerPrintManager;

	public static CallbackContext mCallbackContext;
	public static PluginResult mPluginResult;

	/**
	 * Constructor.
	 */
	public FingerprintAuth() {
	}

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 *
	 * @param cordova
	 *            The context of the main Activity.
	 * @param webView
	 *            The CordovaWebView Cordova is running in.
	 */

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Log.v(TAG, "Init FingerprintAuth");
		packageName = cordova.getActivity().getApplicationContext().getPackageName();
		mPluginResult = new PluginResult(PluginResult.Status.NO_RESULT);

		if (android.os.Build.VERSION.SDK_INT < 23) {
			return;
		}

		mFingerPrintManager = cordova.getActivity().getApplicationContext()
				.getSystemService(FingerprintManager.class);
	}

	/**
	 * Executes the request and returns PluginResult.
	 *
	 * @param action            The action to execute.
	 * @param args              JSONArry of arguments for the plugin.
	 * @param callbackContext   The callback id used when calling back into JavaScript.
	 * @return                  A PluginResult object with a status and message.
	 */
	public boolean execute(final String action,
						   JSONArray args,
						   CallbackContext callbackContext) throws JSONException {
		mCallbackContext = callbackContext;
		Log.v(TAG, "FingerprintAuth action: " + action);
		if (android.os.Build.VERSION.SDK_INT < 23) {
			Log.e(TAG, "minimum SDK version 23 required");
			mPluginResult = new PluginResult(PluginResult.Status.ERROR);
			mCallbackContext.error("minimum SDK version 23 required");
			mCallbackContext.sendPluginResult(mPluginResult);
			return true;
		}

		JSONObject arg_object = args.getJSONObject(0);

		if (action.equals("authenticate")) {
			if (!arg_object.has("clientId") || !arg_object.has("clientSecret")) {
				mPluginResult = new PluginResult(PluginResult.Status.ERROR);
				mCallbackContext.error("Missing required parameters");
				mCallbackContext.sendPluginResult(mPluginResult);
				return true;
			}
			if (isFingerprintAuthAvailable()) {
				cordova.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						// Set up the crypto object for later. The object will be authenticated by use
						// of the fingerprint.

						mFragment = new FingerprintAuthenticationDialogFragment();
						mFragment.setCancelable(false);
						// Show the fingerprint dialog. The user has the option to use the fingerprint with
						// crypto, or you can fall back to using a server-side verified password.
						mFragment.setCryptoObject(null);
						mFragment.show(cordova.getActivity().getFragmentManager(), DIALOG_FRAGMENT_TAG);
					}
				});
				mPluginResult.setKeepCallback(true);
				mCallbackContext.sendPluginResult(mPluginResult);

			} else {
				mPluginResult = new PluginResult(PluginResult.Status.ERROR);
				mCallbackContext.error("Fingerprint authentication not available");
				mCallbackContext.sendPluginResult(mPluginResult);
			}
			return true;
		} else if (action.equals("availability")) {
			JSONObject resultJson = new JSONObject();
			resultJson.put("isAvailable", isFingerprintAuthAvailable());
			resultJson.put("isHardwareDetected", mFingerPrintManager.isHardwareDetected());
			resultJson.put("hasEnrolledFingerprints", mFingerPrintManager.hasEnrolledFingerprints());
			mPluginResult = new PluginResult(PluginResult.Status.OK);
			mCallbackContext.success(resultJson);
			mCallbackContext.sendPluginResult(mPluginResult);
			return true;
		}
		return false;
	}

	private boolean isFingerprintAuthAvailable() {
		return mFingerPrintManager.isHardwareDetected()
				&& mFingerPrintManager.hasEnrolledFingerprints();
	}

}
