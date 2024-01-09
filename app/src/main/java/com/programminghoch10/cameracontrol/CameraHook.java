package com.programminghoch10.cameracontrol;

import android.hardware.Camera;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("deprecation")
public class CameraHook {
	private static int realCameraCount = 0;
	
	static void disableHook(XC_LoadPackage.LoadPackageParam lpparam) {
		PackageHook.CameraPreferences cameraPreferences = new PackageHook.CameraPreferences();
		cameraPreferences.disableAll();
		hook(lpparam, cameraPreferences);
	}
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraPreferences.blockList || cameraPreferences.blockAccess) {
			XposedBridge.log("Hooking getNumberOfCameras");
			XposedHelpers.findAndHookMethod(Camera.class, "getNumberOfCameras", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					realCameraCount = (int) param.getResult();
					int availableCameras = getAvailableCameraCount(cameraPreferences);
					XposedBridge.log("camera1 getNumberOfCameras " + availableCameras + " real="+realCameraCount);
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
					List<Integer> list = generateCameraIdList(cameraPreferences);
					int requestedCameraId = (int) param.args[0];
					XposedBridge.log("camera1 open " + requestedCameraId);
					if (!list.contains(requestedCameraId) && cameraPreferences.blockAccess)
							param.setThrowable(new RuntimeException("denied"));
					XposedBridge.log("camera1 open " + requestedCameraId);
				}
			});
			XposedHelpers.findAndHookMethod(Camera.class, "open", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					XposedBridge.log("camera1 open without args");
					if (cameraPreferences.swapSide ? cameraPreferences.disableFrontFacing : cameraPreferences.disableBackFacing)
						param.setResult(null);
				}
			});
			XposedBridge.log("Hooking getCameraInfo");
			XposedHelpers.findAndHookMethod(Camera.class, "getCameraInfo", int.class, Camera.CameraInfo.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int id = (int) param.args[0];
					param.args[0] = generateCameraIdList(cameraPreferences).get(id);
					if (Objects.isNull(param.args[0])) param.setThrowable(new RuntimeException("denied"));
					XposedBridge.log("camera1 fix getCameraInfo id pre="+id+" post="+param.args[0]);
				}
			});
		}
		if (cameraPreferences.blockFlash) {
			XposedBridge.log("Hooking setFlashMode");
			XposedHelpers.findAndHookMethod(Camera.class, "setFlashMode", String.class, XC_MethodReplacement.DO_NOTHING);
			XposedBridge.log("Hooking getSupportedFlashModes");
			XposedHelpers.findAndHookMethod(Camera.class, "getSupportedFlashModes", XC_MethodReplacement.returnConstant(null));
		}
		if (cameraPreferences.swapSide) {
			XposedBridge.log("camera1 swap side hook");
			XposedBridge.log("Hooking getCameraInfo");
			XposedHelpers.findAndHookMethod(Camera.class, "getCameraInfo", int.class, Camera.CameraInfo.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Camera.CameraInfo cameraInfo = (Camera.CameraInfo) param.args[1];
					XposedBridge.log("camera1 swap side switch now");
					int previousFacing = cameraInfo.facing;
					if (previousFacing == Camera.CameraInfo.CAMERA_FACING_BACK)
						cameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
					if (previousFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
						cameraInfo.facing = Camera.CameraInfo.CAMERA_FACING_BACK;
					if (previousFacing != cameraInfo.facing)
						cameraInfo.orientation = (cameraInfo.orientation + 180) % 360;
				}
			});
		}
	}

	private static boolean shouldDisableCamera(Camera.CameraInfo cameraInfo, PackageHook.CameraPreferences cameraPreferences) {
		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && cameraPreferences.disableFrontFacing)
			return true;
		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && cameraPreferences.disableBackFacing)
			return true;
		return false;
	}

	private static void nativeGetCameraInfo(int i, Camera.CameraInfo cameraInfo) {
		// direct calling the native implementation prevents cyclic recursion somehow
		Method nativeGetCameraInfo = XposedHelpers.findMethodExact(Camera.class, "_getCameraInfo", int.class, boolean.class, Camera.CameraInfo.class);
		try {
			nativeGetCameraInfo.invoke(null, i, false, cameraInfo);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<Integer> generateCameraIdList(PackageHook.CameraPreferences cameraPreferences) {
		List<Integer> list = new ArrayList<>(realCameraCount);
		for (int i = 0; i < realCameraCount; i++) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			nativeGetCameraInfo(i, cameraInfo);
			boolean disableCamera = shouldDisableCamera(cameraInfo, cameraPreferences);
			if (!disableCamera) list.add(i);
		}
		XposedBridge.log("camera1 api map post = " + list);
		return list;
	}
	
	private static int getAvailableCameraCount(PackageHook.CameraPreferences cameraPreferences) {
		List<Integer> list = generateCameraIdList(cameraPreferences);
		if (cameraPreferences.blockList) {
			for (int i = 0; i < list.size(); i++) {
				if (i > list.get(i))
					return i;
			}
		}
		return list.size();
	}
}
