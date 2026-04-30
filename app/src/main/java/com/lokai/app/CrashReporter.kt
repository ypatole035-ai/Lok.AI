package com.lokai.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReporter(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val default = Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val log = buildString {
                appendLine("=== CRASH REPORT $timestamp ===")
                appendLine("Device: \${Build.MODEL} (Android \${Build.VERSION.SDK_INT})")
                appendLine("Thread: \${thread.name}")
                appendLine()
                appendLine(sw.toString())
            }
            val file = File(context.getExternalFilesDir(null), "crash_\$timestamp.txt")
            file.writeText(log)
        } catch (e: Exception) { /* ignore */ }
        default?.uncaughtException(thread, throwable)
    }
}
