package com.programminghoch10.cameracontrol;

import android.content.SharedPreferences;
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
		CameraPreferences cameraPreferences = new CameraPreferences(sharedPreferences);
		
		if (sharedPreferences.getBoolean("disableCameraManager", false)) {
			CameraManagerHook.disableHook(lpparam);
			CameraHook.disableHook(lpparam);
		} else {
			CameraManagerHook.hook(lpparam, cameraPreferences);
			CameraHook.hook(lpparam, cameraPreferences);
		}
	}
	
	public static class CameraPreferences {
		boolean disableFrontFacing = true;
		boolean disableBackFacing = true;
		boolean disableExternal = true;
		boolean blockList = true;
		boolean blockAccess = true;
		boolean blockFlash = true;
		boolean swapSide = false;
		
		CameraPreferences(SharedPreferences sharedPreferences) {
			disableFrontFacing = sharedPreferences.getBoolean("disableFrontFacing", true);
			disableBackFacing = sharedPreferences.getBoolean("disableBackFacing", true);
			disableExternal = sharedPreferences.getBoolean("disableExternal", true);
			blockList = sharedPreferences.getBoolean("blockList", true);
			blockAccess = sharedPreferences.getBoolean("blockAccess", true);
			blockFlash = sharedPreferences.getBoolean("blockFlash", true);
			swapSide = sharedPreferences.getBoolean("swapSide", false);
		}
		
		CameraPreferences() {
		}
		
		void setAll(boolean state) {
			disableFrontFacing = state;
			disableBackFacing = state;
			disableExternal = state;
			blockList = state;
			blockAccess = state;
			blockFlash = state;
		}
		
		void disableAll() {
			setAll(true);
		}
		
		void enableAll() {
			setAll(false);
		}
		
	}
}
