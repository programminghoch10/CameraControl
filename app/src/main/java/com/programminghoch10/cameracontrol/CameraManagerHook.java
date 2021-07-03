package com.programminghoch10.cameracontrol;

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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CameraManagerHook {
	
	static void disableHook(XC_LoadPackage.LoadPackageParam lpparam) {
		XposedBridge.log("Disabling CameraManager completely");
		XposedHelpers.setStaticBooleanField(
				XposedHelpers.findClass("android.hardware.camera2.CameraManager$CameraManagerGlobal", lpparam.classLoader),
				"sCameraServiceDisabled", true
		);
	}
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraPreferences.blockList) {
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
						if (disableCamera(characteristics, cameraPreferences))
							iterator.remove();
					}
					String[] camerasModified = cameras.toArray(new String[0]);
					param.setResult(camerasModified);
				}
			});
		}
		if (cameraPreferences.blockAccess) {
			XposedBridge.log("Hooking openCamera");
			Method openCamera = XposedHelpers.findMethodExact("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera",
					String.class, "android.hardware.camera2.CameraDevice$StateCallback", Handler.class);
			XposedBridge.hookMethod(openCamera, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String cameraId = (String) param.args[0];
					CameraManager cameraManager = (CameraManager) param.thisObject;
					CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
					if (disableCamera(characteristics, cameraPreferences))
						param.setThrowable(new CameraAccessException(CameraAccessException.CAMERA_DISABLED));
				}
			});
		}
	}
	
	private static boolean disableCamera(CameraCharacteristics characteristics, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraPreferences.disableFrontFacing
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT))
			return true;
		if (cameraPreferences.disableBackFacing
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_BACK))
			return true;
		if (cameraPreferences.disableExternal
				&& characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_EXTERNAL))
			return true;
		return false;
	}
}
