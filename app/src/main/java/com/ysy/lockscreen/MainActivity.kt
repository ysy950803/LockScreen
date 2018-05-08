package com.ysy.lockscreen

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process

class MainActivity : Activity() {

    private lateinit var mPolicyManager: DevicePolicyManager
    private var mComponentName: ComponentName? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        tryLockScreen()
        setContentView(R.layout.activity_main)
    }

    private fun init() {
        mPolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        mComponentName = ComponentName(this, AdminReceiver::class.java)
    }

    private fun tryLockScreen() {
        if (!mPolicyManager.isAdminActive(mComponentName)) {
            activeManager()
        } else {
            lockScreenNow()
        }
        killSelf()
    }

    private fun activeManager() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活后即可使用一键锁屏哦(*^__^*)")
        startActivity(intent)
    }

    private fun lockScreenNow() {
        mPolicyManager.lockNow()
    }

    private fun killSelf() {
        finish()
        Process.killProcess(Process.myPid())
    }
}
