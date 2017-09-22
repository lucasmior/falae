package com.marcelorcorrea.falae.task

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.marcelorcorrea.falae.R
import com.marcelorcorrea.falae.model.User
import com.marcelorcorrea.falae.storage.FileHandler
import com.marcelorcorrea.falae.toFile
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Created by corream on 15/05/2017.
 */

class DownloadTask(private val context: Context, private val onSyncComplete: (user: User) -> Unit) : AsyncTask<User, Void, User>() {
    private val executor: ThreadPoolExecutor
    private val numberOfCores: Int = Runtime.getRuntime().availableProcessors()
    private var pDialog: ProgressDialog? = null

    init {
        executor = ThreadPoolExecutor(
                numberOfCores * 2,
                numberOfCores * 2,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue()
        )
    }

    override fun onPreExecute() {
        try {
            if (pDialog != null) {
                pDialog = null
            }
            pDialog = ProgressDialog(context)
            pDialog!!.setMessage(context.getString(R.string.synchronize_message))
            pDialog!!.isIndeterminate = false
            pDialog!!.setCancelable(false)
            pDialog!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun doInBackground(vararg params: User): User? {
        if (!hasNetworkConnection()) {
            return null
        }
        val user = params[0]
        val folder = FileHandler.createUserFolder(context, user.email)
        user.spreadsheets
                .flatMap { it.pages }
                .flatMap { it.items }
                .forEach {
                    executor.execute {
                        Log.d("DEBUG", "Downloading item: " + it.name)
                        val uri = download(folder, it.name, it.imgSrc)
                        it.imgSrc = uri
                    }
                }
        executor.shutdown()
        try {
            executor.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return user
    }

    private fun download(folder: File, name: String, imgSrc: String): String {
        val file = FileHandler.createImg(folder, name, imgSrc)
        val url = URL(imgSrc)
        try {
            val connection = url.openConnection()
            connection.connectTimeout = TIME_OUT
            connection.readTimeout = TIME_OUT
            connection.connect()
            url.openStream().toFile(file.absolutePath)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        return Uri.fromFile(file).toString()
    }

    override fun onPostExecute(user: User?) {
        if (user != null) {
            onSyncComplete(user)
        } else {
            Toast.makeText(context, context.getString(R.string.download_failed), Toast.LENGTH_LONG).show()
        }
        if (pDialog != null && pDialog!!.isShowing) {
            pDialog!!.dismiss()
        }
    }

    private fun hasNetworkConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val allNetworks = cm.allNetworks
            allNetworks
                    .map { cm.getNetworkInfo(it) }
                    .filter { isConnected(it) }
                    .forEach { return true }
        } else {
            val netInfo = cm.allNetworkInfo
            netInfo
                    .filter { isConnected(it) }
                    .forEach { return true }
        }
        return false
    }

    private fun isConnected(networkInfo: NetworkInfo): Boolean =
            (networkInfo.type == ConnectivityManager.TYPE_WIFI || networkInfo.type == ConnectivityManager.TYPE_MOBILE) && networkInfo.isConnected

    companion object {

        private val TIME_OUT = 6000
    }
}