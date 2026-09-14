package com.organisator.print3d

import android.app.Application
import com.organisator.print3d.notifications.Notifications

class OrganisatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
