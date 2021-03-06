package com.navigation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import java.io.Serializable

/**
 * Created by a.lunkov on 25.10.2017.
 */

object Converter {

    internal fun readIntent(current: Context?, source: Map<*, *>, callback: Callback): Intent? {
        val result = createIntent(
                current,
                source["packageName"] as String?,
                source["className"] as String?,
                source["action"] as String?,
                source["customServiceEventName"] as String?,
                callback
        )
        if (result != null) {
            val extrasSource = source["extras"] as Map<*, *>
            if (!extrasSource.isEmpty()) {
                val extrasResult = Bundle()
                for (key in extrasSource.keys) {
                    readIntentExtra(key as String, extrasSource[key], extrasResult)
                }
                if (result.hasExtra("type")) {
                    result.putExtra("extras", extrasResult)
                } else {
                    result.putExtras(extrasResult)
                }
            }
            for (category in source["categories"] as List<*>) {
                result.addCategory(category as String)
            }
            for (flag in source["flags"] as List<*>) {
                result.addFlags(flag as? Int ?: (flag as Double).toInt())
            }
        }
        return result
    }

    private fun createIntent(current: Context?,
                             packageName: String?,
                             className: String?,
                             action: String?,
                             customServiceEventName: String?,
                             callback: Callback): Intent? {
        var result: Intent? = null
        if (customServiceEventName != null) {
            result = if (packageName != null) {
                createIntentWithPackageAndClass(
                        current!!.packageManager,
                        packageName,
                        "com.navigation.EventEmissionService",
                        callback
                )
            } else {
                Intent(current, EventEmissionService::class.java)
            }
            if (result != null) {
                result.putExtra("type", customServiceEventName)
            }
        } else {
            if (className != null) {
                if (packageName != null) {
                   result = createIntentWithPackageAndClass(
                           current!!.packageManager,
                           packageName,
                           className,
                           callback
                   )
                } else {
                    try {
                        val dest = Class.forName(className)
                        result = Intent(current, dest)
                    } catch (e: ClassNotFoundException) {
                        callback.invoke(writeError("TARGET_CLASS_NOT_FOUND"))
                    }
                }

            } else if (packageName != null) {
                result = current!!.packageManager.getLaunchIntentForPackage(packageName)
                if (result == null) {
                    callback.invoke(writeError("TARGET_PACKAGE_NOT_FOUND"))
                }
            } else {
                result = Intent()
            }
            if (result != null && action != null) {
                result.action = action
            }
        }
        return result
    }

    private fun createIntentWithPackageAndClass(manager: PackageManager,
                                                packageName: String,
                                                className: String,
                                                callback: Callback): Intent? {
        var result: Intent? = null
        try {
            manager.getPackageInfo(packageName, 0)
            result = Intent()
        } catch (e: PackageManager.NameNotFoundException) {
            callback.invoke(writeError("TARGET_PACKAGE_NOT_FOUND"))
        }
        if (result != null) {
            result.component = ComponentName(packageName, className)
            var list = manager.queryIntentActivities(
                    result,
                    PackageManager.MATCH_DEFAULT_ONLY
            )
            if (list.size == 0) {
                list = manager.queryIntentServices(
                        result,
                        PackageManager.MATCH_DEFAULT_ONLY
                )
                if(list.size == 0) {
                    result = null
                    callback.invoke(writeError("TARGET_CLASS_NOT_FOUND"))
                }
            }
        }
        return result
    }


    private fun readIntentExtra(key: String, item: Any?, extras: Bundle) {
        when (item) {
            is Boolean -> extras.putBoolean(key, item)
            is Int -> extras.putInt(key, item)
            is Double -> extras.putDouble(key, item)
            is String -> extras.putString(key, item)
            is Map<*, *>, is List<*> -> extras.putSerializable(key, item as Serializable?)
        }
    }

    internal fun writeIntent(source: Intent): WritableMap {
        val result = Arguments.createMap()
        result.putString("className", if (source.component == null) null else source.component.className)
        result.putString("packageName", source.`package`)
        result.putString("action", source.action)
        result.putMap("extras", if (source.extras == null) null else writeIntentExtras(source.extras))
        val categories = Arguments.createArray()
        if (source.categories != null) {
            for (item in source.categories) {
                categories.pushString(item)
            }
        }
        result.putArray("categories", categories)
        result.putInt("flags", source.flags)
        return result
    }

    internal fun writeIntentExtras(source: Bundle): WritableMap {
        val result = Arguments.createMap()
        for (key in source.keySet()) {
            writeIntentExtraObjectItem(key, source.get(key), result)
        }
        return result
    }

    private fun writeIntentExtraObjectItem(key: String, item: Any?, result: WritableMap) {
        when (item) {
            is Nothing -> result.putNull(key)
            is Boolean -> result.putBoolean(key, item)
            is Int -> result.putInt(key, item)
            is Double -> result.putDouble(key, item)
            is String -> result.putString(key, item)
            is Map<*, *> -> result.putMap(key, writeIntentExtraObject(item))
            is List<*> -> result.putArray(key, writeIntentExtraArray(item))
            is WritableMap -> result.putMap(key, item)
            is WritableArray -> result.putArray(key, item)
            is Bundle -> result.putMap(key, writeIntentExtras(item))
        }
    }

    private fun writeIntentExtraObject(source: Map<*, *>): WritableMap {
        val result = Arguments.createMap()
        for (key in source.keys) {
            writeIntentExtraObjectItem(key as String, source[key], result)
        }
        return result
    }

    private fun writeIntentExtraArray(source: List<*>): WritableArray {
        val result = Arguments.createArray()
        for (item in source) {
            when (item) {
                is Nothing -> result.pushNull()
                is Boolean -> result.pushBoolean(item)
                is Int -> result.pushInt(item)
                is Double -> result.pushDouble(item)
                is String -> result.pushString(item)
                is Map<*, *> -> result.pushMap(writeIntentExtraObject(item))
                is List<*> -> result.pushArray(writeIntentExtraArray(item))
                is WritableMap -> result.pushMap(item)
                is WritableArray -> result.pushArray(item)
                is Bundle -> result.pushMap(writeIntentExtras(item))
            }
        }
        return result
    }

    internal fun writeLocalEvent(eventName: String, vararg data: Any?): WritableMap {
        val result: WritableMap = Arguments.createMap()
        result.putString("type", eventName)
        result.putArray("extras", writeIntentExtraArray(data.asList()))
        return result
    }

    internal fun writeError(message: String?): WritableMap {
        val result = Arguments.createMap()
        result.putString("error", message)
        return result
    }

}
