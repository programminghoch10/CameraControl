package com.programminghoch10.cameracontrol;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CameraManagerHook {
	
	static void disableHook(XC_LoadPackage.LoadPackageParam lpparam) {
		XposedBridge.log("Disabling CameraManager completely");
		XposedHelpers.setStaticBooleanField(
				XposedHelpers.findClass(CameraManager.class.getName() + "$CameraManagerGlobal", lpparam.classLoader),
				"sCameraServiceDisabled", true
		);
	}
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraPreferences.blockList) {
			XposedBridge.log("Hooking getCameraIdList");
			XposedHelpers.findAndHookMethod(CameraManager.class, "getCameraIdList", new XC_MethodHook() {
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
			Method openCamera = XposedHelpers.findMethodExact(CameraManager.class, "openCamera",
					String.class, CameraDevice.StateCallback.class, Handler.class);
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
		if (cameraPreferences.blockFlash) {
			XposedBridge.log("Hooking setTorchMode");
			XposedHelpers.findAndHookMethod(CameraManager.class, "setTorchMode", String.class, boolean.class, XC_MethodReplacement.DO_NOTHING);
			XposedBridge.log("Hooking get");
			XposedHelpers.findAndHookMethod(CameraCharacteristics.class, "get", CameraCharacteristics.Key.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					CameraCharacteristics.Key<?> key = (CameraCharacteristics.Key<?>) param.args[0];
					Log.d("CameraControl", "beforeHookedMethod: get key " + key.getName());
					if (key.getName().equals(CameraCharacteristics.FLASH_INFO_AVAILABLE.getName())) {
						Log.d("CameraControl", "beforeHookedMethod: PREVENT FLASH");
						param.setResult(false);
					}
				}
				
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					CameraCharacteristics.Key<?> key = (CameraCharacteristics.Key<?>) param.args[0];
					if (key.getName().equals(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES.getName())) {
						Log.d("CameraControl", "afterHookedMethod: filtering flash");
						int[] methodResult = (int[]) param.getResult();
						int[] filteredResult =
								Arrays.stream(methodResult).filter(u -> {
									switch (u) {
										case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH:
										case CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
										case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE:
										case CameraCharacteristics.CONTROL_AE_MODE_ON_EXTERNAL_FLASH:
											return false;
										default:
											return true;
									}
								}).toArray();
						param.setResult(filteredResult);
					}
				}
			});
			XposedBridge.log("Hooking registerTorchCallback");
			XposedHelpers.findAndHookMethod(CameraManager.class, "registerTorchCallback",
					CameraManager.TorchCallback.class, Executor.class, XC_MethodReplacement.DO_NOTHING);
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
