package ru.netology.nmedia.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// !!! не забыть прописать в AndroidManifest.xml: android:name=".application.NMediaApp" !!!
@HiltAndroidApp
class NMediaApp : Application()