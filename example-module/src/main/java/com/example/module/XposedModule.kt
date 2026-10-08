@file:JvmName("XposedModule") // Prevent kotlin from renaming the file
package com.example.module

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

class XposedModule : IXposedHookLoadPackage {
    companion object {
        private const val TARGET_APP = "com.example.app"

        // Important to place this in a singleton, XposedModule may be created multiple times.
        private val hasLoaded = AtomicBoolean(false)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) return

        // Prevent hooks from being registered twice
        if (hasLoaded.getAndSet(true)) {
            return
        }

        // Account for module being loaded into your own app
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            XposedBridge.log("Self-loaded module with version ${BuildConfig.VERSION_NAME}")

            // You cannot do MainActivity.isLoaded = true
            // This is because it will set the module reference, not the app reference.
            // To impact the app you need to use XposedHelpers or XposedBridge
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
        // XposedHelpers.findAndHookMethod(...)
    }
}