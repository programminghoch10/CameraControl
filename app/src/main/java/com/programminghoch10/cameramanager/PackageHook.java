package com.programminghoch10.cameramanager;

import android.content.SharedPreferences;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PackageHook implements IXposedHookLoadPackage {
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) return;
		SharedPreferences sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, "camera");
		if (!sharedPreferences.getBoolean("available", false)) return;
		if (sharedPreferences.getBoolean("disableCameraManager", false)) {
			DisableHook.hook(lpparam);
		} else {
			CameraManagerHook.hook(lpparam, sharedPreferences);
		}
	}
}
