package ru.netology.nmedia.application

import android.app.Application
import ru.netology.nmedia.auth.AppAuth

// !!! не забыть прописать в AndroidManifest.xml: android:name=".application.NMediaApp" !!!
class NMediaApp: Application() {
    override fun onCreate() {
        super.onCreate()
        AppAuth.initApp(this)
    }
}