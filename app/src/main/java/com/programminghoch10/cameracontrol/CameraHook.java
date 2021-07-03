package com.programminghoch10.cameracontrol;

import android.hardware.Camera;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CameraHook {
	private static int realCameraCount = 0;
	
	static void disableHook(XC_LoadPackage.LoadPackageParam lpparam) {
		PackageHook.CameraPreferences cameraPreferences = new PackageHook.CameraPreferences();
		cameraPreferences.disableAll();
		hook(lpparam, cameraPreferences);
	}
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraPreferences.blockLegacy()) {
			XposedBridge.log("Hooking getNumberOfCameras");
			XposedHelpers.findAndHookMethod(Camera.class, "getNumberOfCameras", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					realCameraCount = (int) param.getResult();
					int availableCameras = getAvailableCamerasFromIdMap(generateIdMap(cameraPreferences));
					param.setResult(availableCameras);
				}
			});
			XposedBridge.log("Hooking shouldExposeAuxCamera");
			XposedHelpers.findAndHookMethod(Camera.class, "shouldExposeAuxCamera", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (cameraPreferences.disableExternal) param.setResult(false);
				}
			});
			XposedBridge.log("Hooking open");
			XposedHelpers.findAndHookMethod(Camera.class, "open", int.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Map<Integer, Integer> map = generateIdMap(cameraPreferences);
					int requestedCameraId = (int) param.args[0];
					if (!map.containsKey(requestedCameraId))
						param.setThrowable(new RuntimeException());
					Integer resultingCameraId = map.get(requestedCameraId);
					if (resultingCameraId == null) param.setThrowable(new RuntimeException());
					param.args[0] = resultingCameraId;
				}
			});
			XposedHelpers.findAndHookMethod(Camera.class, "open", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (cameraPreferences.disableBackFacing) param.setResult(null);
				}
			});
		}
		if (cameraPreferences.blockFlash) {
			XposedBridge.log("Hooking setFlashMode");
			XposedHelpers.findAndHookMethod(Camera.class, "setFlashMode", String.class, XC_MethodReplacement.DO_NOTHING);
			XposedBridge.log("Hooking getSupportedFlashModes");
			XposedHelpers.findAndHookMethod(Camera.class, "getSupportedFlashModes", XC_MethodReplacement.returnConstant(null));
		}
	}
	
	private static boolean shouldDisableCamera(Camera.CameraInfo cameraInfo, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && cameraPreferences.disableFrontFacing)
			return true;
		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && cameraPreferences.disableBackFacing)
			return true;
		return false;
	}
	
	private static Map<Integer, Integer> generateIdMap(PackageHook.CameraPreferences cameraPreferences) {
		Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < realCameraCount; i++) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(i, cameraInfo);
			boolean disableCamera = shouldDisableCamera(cameraInfo, cameraPreferences);
			map.put(i, disableCamera ? null : i);
		}
		return map;
	}
	
	private static int getAvailableCamerasFromIdMap(Map<Integer, Integer> map) {
		int cameraCount = 0;
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue() != null) cameraCount++;
		}
		return cameraCount;
	}
}
