package com.programminghoch10.cameramanager;

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
	private static final String TAG = "CameraManager";
	
	static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
		XposedBridge.log("Hooking getCameraIdList");
		XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "getCameraIdList", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				String[] ids = (String[]) param.getResult();
				String s = Arrays.toString(ids);
				//Log.d(TAG, "getCameraIdList: " + s);
				//XposedBridge.log("getCameraIdList: " + s);
				CameraManager cameraManager = (CameraManager) param.thisObject;
				List<String> cameras = new ArrayList<>(Arrays.asList(ids));
				Iterator<String> iterator = cameras.iterator();
				while (iterator.hasNext()) {
					String id = iterator.next();
					CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
					if (characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT))
						iterator.remove();
				}
				String[] camerasModified = cameras.toArray(new String[0]);
				param.setResult(camerasModified);
				//Log.d(TAG, "getCameraIdList: after modification " + Arrays.toString(camerasModified));
			}
		});
		XposedBridge.log("Hooking openCamera");
		Method openCamera = XposedHelpers.findMethodExact("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera",
				String.class, "android.hardware.camera2.CameraDevice$StateCallback", Handler.class);
		XposedBridge.hookMethod(openCamera, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				String cameraId = (String) param.args[0];
				//Log.d(TAG, "openCamera: " + cameraId);
				CameraManager cameraManager = (CameraManager) param.thisObject;
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				if (characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT))
					param.setThrowable(new CameraAccessException(CameraAccessException.CAMERA_DISABLED));
			}
		});
	}
}
