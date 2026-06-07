package dev.moonpic.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt's annotation generates the DI graph.
 */
@HiltAndroidApp
class MoonPicApp : Application()
