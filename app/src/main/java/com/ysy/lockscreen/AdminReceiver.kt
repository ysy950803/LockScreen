package com.ysy.lockscreen

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {

    private val TAG = "AdminReceiver"

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive")
    }

    override fun onEnabled(context: Context?, intent: Intent?) {
        super.onEnabled(context, intent)
        Toast.makeText(context!!.applicationContext, "激活成功", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onEnabled")
    }

    override fun onDisabled(context: Context?, intent: Intent?) {
        super.onDisabled(context, intent)
        Toast.makeText(context!!.applicationContext, "取消激活", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onDisabled")
    }
}
