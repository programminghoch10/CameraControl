package com.programminghoch10.cameramanager;

import android.annotation.SuppressLint;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OwnHook {
	static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
		XposedHelpers.setStaticBooleanField(
				XposedHelpers.findClass(BuildConfig.APPLICATION_ID + ".SettingsActivity", lpparam.classLoader),
				"xposedActive", true);
	}
}
