package com.example.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VpnActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == VpnNotificationHelper.ACTION_DISCONNECT) {
            VpnServiceManager.disconnect(context)
        }
    }
}
