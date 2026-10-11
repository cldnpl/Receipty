package com.cldnpl.receipty

import android.app.Application
import com.cldnpl.receipty.localization.I18n
import com.cldnpl.receipty.models.AppSettings

class ReceiptyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        I18n.init(this)
    }
}
