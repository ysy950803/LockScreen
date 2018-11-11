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
import android.view.KeyEvent
import android.view.View
import android.widget.Toast

class MainActivity : Activity() {

    private val TAG = "MainActivity"
    private val REQUEST_CODE_WRITE_SETTINGS = 1

    private lateinit var mPolicyManager: DevicePolicyManager

    private var mOriginalLockTime: Int = 0
    private var mIsNewLockMode: Boolean = false
    private var mIsRequestingPerm: Boolean = false

    private var mComponentName: ComponentName? = null
    private var mScreenStatReceiver: ScreenStatReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        tryLockScreen()
    }

    override fun onPause() {
        super.onPause()
        if (!mIsRequestingPerm) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenStat()
        resetLockTime()
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
        return Build.VERSION.SDK_INT > 23 && Build.MANUFACTURER != "Xiaomi"
    }

    private fun canWriteSystem(): Boolean {
        return Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(this)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestWriteSettingsPerm() {
        mIsRequestingPerm = true
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
        Toast.makeText(this, "允许后按返回即可使用一键锁屏(*^__^*)", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mIsRequestingPerm = false
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Build.VERSION.SDK_INT >= 23) {
            if (Settings.System.canWrite(this)) {
                lockScreenDelayed()
            }
        }
    }

    private fun lockScreenDelayed() {
        blackScreen()
        mOriginalLockTime = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 0)
    }

    private fun resetLockTime() {
        if (canWriteSystem()) {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, mOriginalLockTime)
        }
    }

    private fun blackScreen() {
        setWindowBrightness(0)
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun setWindowBrightness(brightness: Int) {
        val window = window
        val lp = window.attributes
        lp.screenBrightness = brightness / 255.0f
        window.attributes = lp
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
                    resetLockTime()
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
