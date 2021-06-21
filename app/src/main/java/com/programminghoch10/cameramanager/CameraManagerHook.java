package com.programminghoch10.cameramanager;

import android.content.SharedPreferences;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CameraManagerHook {
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam, SharedPreferences sharedPreferences) {
		if (sharedPreferences.getBoolean("blockList", true)) {
			XposedBridge.log("Hooking getCameraIdList");
			XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "getCameraIdList", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String[] ids = (String[]) param.getResult();
					CameraManager cameraManager = (CameraManager) param.thisObject;
					List<String> cameras = new ArrayList<>(Arrays.asList(ids));
					Iterator<String> iterator = cameras.iterator();
					while (iterator.hasNext()) {
						String id = iterator.next();
						CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
						if (disableCamera(characteristics, sharedPreferences))
							iterator.remove();
					}
					String[] camerasModified = cameras.toArray(new String[0]);
					param.setResult(camerasModified);
				}
			});
		}
		if (sharedPreferences.getBoolean("blockAccess", true)) {
			XposedBridge.log("Hooking openCamera");
			Method openCamera = XposedHelpers.findMethodExact("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera",
					String.class, "android.hardware.camera2.CameraDevice$StateCallback", Handler.class);
			XposedBridge.hookMethod(openCamera, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String cameraId = (String) param.args[0];
					CameraManager cameraManager = (CameraManager) param.thisObject;
					CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
					if (disableCamera(characteristics, sharedPreferences))
						param.setThrowable(new CameraAccessException(CameraAccessException.CAMERA_DISABLED));
				}
			});
		}
	}
	
	private static boolean disableCamera(CameraCharacteristics characteristics, SharedPreferences sharedPreferences) {
		if (sharedPreferences.getBoolean("disableFrontFacing", true)
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT))
			return true;
		if (sharedPreferences.getBoolean("disableBackFacing", true)
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_BACK))
			return true;
		if (sharedPreferences.getBoolean("disableExternal", true)
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_EXTERNAL))
			return true;
		return false;
	}
}
