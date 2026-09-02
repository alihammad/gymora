package com.gymora

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; Hilt root for dependency injection. */
@HiltAndroidApp
class GymoraApplication : Application()
