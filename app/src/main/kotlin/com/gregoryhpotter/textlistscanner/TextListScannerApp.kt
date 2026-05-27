package com.gregoryhpotter.textlistscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — required by Hilt to generate the dependency graph.
 * Must be declared in AndroidManifest.xml via android:name=".TextListScannerApp"
 */
@HiltAndroidApp
class TextListScannerApp : Application()