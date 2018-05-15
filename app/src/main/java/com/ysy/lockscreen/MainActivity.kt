package com.ysy.lockscreen

import android.annotation.TargetApi
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val TAG = "MainActivty"
    private val REQUEST_CODE_WRITE_SETTINGS = 1

    private lateinit var mPolicyManager: DevicePolicyManager

    private var mOriginalLockTime: Int = 0
    private var mIsNewLockMode: Boolean = false

    private var mComponentName: ComponentName? = null
    private var mScreenStatReceiver: ScreenStatReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        setContentView(R.layout.activity_main)
        tryLockScreen()
    }

    private fun init() {
        mIsNewLockMode = isNewLockMode()
        mPolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mComponentName = ComponentName(this, AdminReceiver::class.java)

        if (mIsNewLockMode) {
            mScreenStatReceiver = ScreenStatReceiver()
            registerScreenStat()
        }
    }

    private fun tryLockScreen() {
        if (mIsNewLockMode) {
            if (canWriteSystem()) {
                lockScreenDelayed()
            } else {
                requestWriteSettingsPerm()
            }
        } else {
            if (!mPolicyManager.isAdminActive(mComponentName)) {
                activeManager()
            } else {
                lockScreenNow()
            }
            killSelf()
        }
    }

    private fun activeManager() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活后即可使用一键锁屏(*^__^*)")
        startActivity(intent)
    }

    private fun lockScreenNow() {
        mPolicyManager.lockNow()
    }

    private fun killSelf() {
        unregisterScreenStat()
        finish()
        Process.killProcess(Process.myPid())
    }

    private fun isNewLockMode(): Boolean {
        return Build.VERSION.SDK_INT >= 23
    }

    private fun canWriteSystem(): Boolean {
        return Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(this)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestWriteSettingsPerm() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
        Toast.makeText(this, "允许后按返回即可使用一键锁屏(*^__^*)", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Build.VERSION.SDK_INT >= 23) {
            if (Settings.System.canWrite(this)) {
                lockScreenDelayed()
            }
        }
    }

    private fun lockScreenDelayed() {
        blackScreen()
        mOriginalLockTime = Settings.System.getInt(contentResolver, "screen_off_timeout")
        Settings.System.putInt(contentResolver, "screen_off_timeout", 0)
    }

    private fun blackScreen() {
        layout_main_bg.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun registerScreenStat() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        this.registerReceiver(mScreenStatReceiver, filter)
    }

    private fun unregisterScreenStat() {
        if (mScreenStatReceiver != null) {
            this.unregisterReceiver(mScreenStatReceiver)
        }
    }

    private inner class ScreenStatReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Intent.ACTION_SCREEN_OFF")
                    if (canWriteSystem()) {
                        Settings.System.putInt(contentResolver, "screen_off_timeout", mOriginalLockTime)
                    }
                    Handler().postDelayed({
                        killSelf()
                    }, 768)
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Intent.ACTION_SCREEN_ON")
                    killSelf()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Intent.ACTION_USER_PRESENT")
                    killSelf()
                }
            }
        }
    }
}
