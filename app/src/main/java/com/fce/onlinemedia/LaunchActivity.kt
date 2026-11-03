package com.fce.onlinemedia

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/** Opens the real player. BYD launches this when you tap the media widget. */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launch = packageManager.getLaunchIntentForPackage(BuildConfig.PLAYER_PACKAGE)
        if (launch != null) {
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Toast.makeText(this, "${BuildConfig.PLAYER_PACKAGE} is not installed", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
