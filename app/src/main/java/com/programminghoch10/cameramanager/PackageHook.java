package com.programminghoch10.cameramanager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PackageHook implements IXposedHookLoadPackage {
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) return;
		//DisableHook.hook(lpparam);
		CameraManagerHook.hook(lpparam);
	}
}
