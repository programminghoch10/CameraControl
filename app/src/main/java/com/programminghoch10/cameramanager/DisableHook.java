package com.programminghoch10.cameramanager;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DisableHook {
	static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
		XposedBridge.log("Disabling CameraManager completely");
		XposedHelpers.setStaticBooleanField(
				XposedHelpers.findClass("android.hardware.camera2.CameraManager$CameraManagerGlobal", lpparam.classLoader),
				"sCameraServiceDisabled", true
		);
	}
}
