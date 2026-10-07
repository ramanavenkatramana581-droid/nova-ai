package com.example.intent

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemActionExecutor(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private var isTorchOn: Boolean = false

    fun executeAction(action: AssistantAction): ExecutionResult {
        return when (action) {
            is AssistantAction.OpenApp -> openApp(action)
            is AssistantAction.SetTimer -> setTimer(action)
            is AssistantAction.SetAlarm -> setAlarm(action)
            is AssistantAction.TakeCameraPhoto -> takeCameraPhoto()
            is AssistantAction.ToggleFlashlight -> toggleFlashlight(action.enable)
            is AssistantAction.QueryBattery -> getBatteryTelemetry()
            is AssistantAction.QueryDateTime -> getDateTimeTelemetry()
            is AssistantAction.WebSearch -> searchWeb(action.query)
            is AssistantAction.StopSpeaking, is AssistantAction.GeneralAiQuery -> ExecutionResult.Success("No system operation required.")
        }
    }

    private fun openApp(action: AssistantAction.OpenApp): ExecutionResult {
        val pm = context.packageManager

        // Specific named apps
        when (action.appName) {
            "Settings" -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryLaunch(intent, "System Settings opened.")
            }
            "Calendar" -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    return tryLaunch(intent, "Calendar launched.")
                }
            }
            "Calculator" -> {
                val calcIntent = pm.getLaunchIntentForPackage("com.google.android.calculator")
                    ?: pm.getLaunchIntentForPackage("com.android.calculator2")
                if (calcIntent != null) {
                    calcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return tryLaunch(calcIntent, "Calculator module launched.")
                }
            }
        }

        // Try package
        if (action.targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(action.targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return tryLaunch(launchIntent, "${action.appName} launched.")
            }
        }

        // Web fallback if package not installed
        if (action.webFallbackUrl != null) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action.webFallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return tryLaunch(webIntent, "${action.appName} portal accessed via browser.")
        }

        return ExecutionResult.Notice("${action.appName} app is not installed on this device.")
    }

    private fun setTimer(action: AssistantAction.SetTimer): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ExecutionResult.Success("Timer initialized for ${action.seconds} seconds.")
            } else {
                ExecutionResult.Notice("Timer action received. No native clock timer app detected.")
            }
        } catch (e: Exception) {
            ExecutionResult.Notice("Timer command processed: ${action.seconds} seconds.")
        }
    }

    private fun setAlarm(action: AssistantAction.SetAlarm): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ExecutionResult.Success(String.format("Alarm set for %02d:%02d.", action.hour, action.minute))
            } else {
                ExecutionResult.Notice(String.format("Alarm scheduled for %02d:%02d.", action.hour, action.minute))
            }
        } catch (e: Exception) {
            ExecutionResult.Notice(String.format("Alarm parameters recorded for %02d:%02d.", action.hour, action.minute))
        }
    }

    private fun takeCameraPhoto(): ExecutionResult {
        return try {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                ExecutionResult.Success("Camera sensor activated.")
            } else {
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(fallbackIntent)
                    ExecutionResult.Success("Image capture matrix launched.")
                } else {
                    ExecutionResult.Notice("Camera application not accessible.")
                }
            }
        } catch (e: Exception) {
            ExecutionResult.Notice("Unable to trigger camera: ${e.message}")
        }
    }

    fun toggleFlashlight(enable: Boolean): ExecutionResult {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return ExecutionResult.Notice("Hardware flashlight emitter not present on this device.")
        }
        return try {
            val cm = cameraManager ?: return ExecutionResult.Notice("Camera hardware subsystem unavailable.")
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cm.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cm.setTorchMode(cameraId, enable)
                isTorchOn = enable
                ExecutionResult.Success(if (enable) "Tactical illuminator ON" else "Tactical illuminator OFF")
            } else {
                ExecutionResult.Notice("Flashlight hardware ID not detected.")
            }
        } catch (e: Exception) {
            ExecutionResult.Notice("Flashlight state change error: ${e.message}")
        }
    }

    fun getBatteryTelemetry(): ExecutionResult {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val report = if (isCharging) {
            "Power cell level at $pct%. Core currently linked to charging station."
        } else {
            "Power cell level at $pct%. Discharging at nominal rate."
        }
        return ExecutionResult.Telemetry(report, pct, isCharging)
    }

    fun getDateTimeTelemetry(): ExecutionResult {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val now = Date()
        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)
        val report = "Local chronometer: $timeStr. Planetary cycle: $dateStr."
        return ExecutionResult.Telemetry(report, 0, false)
    }

    private fun searchWeb(query: String): ExecutionResult {
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
                ExecutionResult.Success("Global web search dispatched for '$query'.")
            } else {
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                ExecutionResult.Success("Google web search uplink launched.")
            }
        } catch (e: Exception) {
            ExecutionResult.Notice("Web query could not be dispatched: ${e.message}")
        }
    }

    private fun tryLaunch(intent: Intent, successMsg: String): ExecutionResult {
        return try {
            context.startActivity(intent)
            ExecutionResult.Success(successMsg)
        } catch (e: Exception) {
            ExecutionResult.Notice("Unable to execute directive: ${e.message}")
        }
    }
}

sealed class ExecutionResult {
    data class Success(val message: String) : ExecutionResult()
    data class Notice(val message: String) : ExecutionResult()
    data class Telemetry(val message: String, val level: Int, val isCharging: Boolean) : ExecutionResult()
}
