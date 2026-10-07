@file:JvmName("XposedModule") // Prevent kotlin from renaming the file
package com.example.module

import com.virtualxposed.maliciousmodule.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedModule : IXposedHookLoadPackage {
    companion object {
        private const val TARGET_APP = "com.example.app"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) return

        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            XposedBridge.log("Self-loaded module with version ${BuildConfig.VERSION_NAME}")
            val buildClass = XposedHelpers.findClass(
                MainActivity::class.java.name,
                lpparam.classLoader
            )

            XposedHelpers.setStaticObjectField(
                buildClass,
                MainActivity::isLoaded.name,
                true
            )
            return
        }

        // Ensure you always target the specific application and never register hooks in an unknown application
        if (lpparam.packageName != TARGET_APP) {
            return
        }

        // Hooks here


    }
}