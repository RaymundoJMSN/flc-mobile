package com.phenomen.flc

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL

// Published by .github/workflows/android.yml on every `v*` tag. GitHub keeps
// /releases/latest/download/<asset> pointing at the newest release, so this URL never changes.
private const val MANIFEST_URL =
  "https://github.com/RaymundoJMSN/flc-mobile/releases/latest/download/android-latest.json"

private const val TAG = "FlcUpdater"
private const val APK_MIME = "application/vnd.android.package-archive"

/**
 * Out-of-store updates: read a version manifest, ask the user, download the APK with the
 * system DownloadManager, then hand it to the system installer.
 *
 * Only works when the new APK is signed with the same key as the installed one.
 */
class Updater(private val activity: Activity) {
  private var pendingApk: File? = null

  fun checkInBackground() {
    Thread {
      try {
        val manifest = JSONObject(URL(MANIFEST_URL).readText())
        if (manifest.getInt("versionCode") <= BuildConfig.VERSION_CODE) return@Thread

        val versionName = manifest.optString("versionName", "?")
        val url = manifest.getString("url")
        val notes = manifest.optString("notes", "")

        activity.runOnUiThread { prompt(versionName, notes, url) }
      } catch (e: Exception) {
        // being offline is the common case here, not worth a dialog
        Log.w(TAG, "update check failed: ${e.message}")
      }
    }.start()
  }

  /** Call from onResume: finishes an update that was waiting on the install permission. */
  fun retryPendingInstall() {
    pendingApk?.let { install(it) }
  }

  private fun prompt(versionName: String, notes: String, url: String) {
    if (activity.isFinishing) return

    AlertDialog.Builder(activity)
      .setTitle("Update available")
      .setMessage(if (notes.isBlank()) "FLC $versionName is available." else "FLC $versionName\n\n$notes")
      .setPositiveButton("Update") { _, _ -> download(versionName, url) }
      .setNegativeButton("Later", null)
      .show()
  }

  private fun download(versionName: String, url: String) {
    val name = "flc-$versionName.apk"
    val target = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name)
    target.delete() // otherwise DownloadManager writes flc-x-1.apk and we install a stale file

    val downloads = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val id = downloads.enqueue(
      DownloadManager.Request(Uri.parse(url))
        .setTitle("FLC $versionName")
        .setDescription("Downloading update")
        .setMimeType(APK_MIME)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, name)
    )

    ContextCompat.registerReceiver(
      activity,
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
          context.unregisterReceiver(this)
          if (downloadSucceeded(downloads, id)) install(target) else Log.w(TAG, "download failed")
        }
      },
      IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
      ContextCompat.RECEIVER_EXPORTED
    )
  }

  private fun downloadSucceeded(downloads: DownloadManager, id: Long): Boolean =
    downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
      cursor.moveToFirst() &&
        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
        DownloadManager.STATUS_SUCCESSFUL
    }

  private fun install(apk: File) {
    // sideloaded installs need an explicit per-app grant since Android 8
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      !activity.packageManager.canRequestPackageInstalls()
    ) {
      pendingApk = apk
      AlertDialog.Builder(activity)
        .setTitle("Allow installing updates")
        .setMessage("Android needs permission to let FLC install its own updates. Enable it and come back.")
        .setPositiveButton("Open settings") { _, _ ->
          activity.startActivity(
            Intent(
              Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
              Uri.parse("package:${activity.packageName}")
            )
          )
        }
        .setNegativeButton("Cancel") { _, _ -> pendingApk = null }
        .show()
      return
    }

    pendingApk = null
    val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
    activity.startActivity(
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, APK_MIME)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    )
  }
}
