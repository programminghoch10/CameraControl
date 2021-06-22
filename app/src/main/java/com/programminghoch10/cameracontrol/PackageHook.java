package com.programminghoch10.cameracontrol;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PackageHook implements IXposedHookLoadPackage {
	
	private static XSharedPreferences getSharedPreferences() {
		XSharedPreferences sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, "camera");
		if (!sharedPreferences.getFile().canRead()) sharedPreferences = null;
		if (sharedPreferences == null) {
			Log.e("CameraControl", "getSharedPreferences: failed to load SharedPreferences");
		}
		return sharedPreferences;
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) return;
		if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			OwnHook.hook(lpparam);
			return;
		}
		
		//Log.d("CameraControl", "handleLoadPackage: package="+lpparam.packageName);
		//XposedBridge.log("handleLoadPackage: package="+lpparam.packageName);
		
		XSharedPreferences sharedPreferences = getSharedPreferences();
		if (sharedPreferences == null) return;
		
		if (sharedPreferences.getBoolean("disableCameraManager", false)) {
			DisableHook.hook(lpparam);
		} else {
			CameraManagerHook.hook(lpparam, sharedPreferences);
		}
	}
}
