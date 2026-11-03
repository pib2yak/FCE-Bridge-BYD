package com.fce.onlinemedia.voice

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.fce.onlinemedia.BuildConfig
import java.io.File

/**
 * BYD's media app binds here (it hard-codes this class name for its "FCE" partner apps).
 * We connect to the real player (Auxio / Retro under a borrowed package name),
 * mirror its now-playing info into our own session, and forward BYD's
 * play/pause/next/previous back to the player.
 * Album art is saved to a file and offered through ArtProvider, because BYD
 * only reads the artwork *URI* from the metadata.
 */
class CarMediaTransportService : MediaBrowserServiceCompat() {

    private val tag = "FceBridge"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var session: MediaSessionCompat
    private var browser: MediaBrowserCompat? = null
    private var controller: MediaControllerCompat? = null
    private val pending = mutableListOf<(MediaControllerCompat) -> Unit>()

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, tag).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = withPlayer { it.transportControls.play() }
                override fun onPause() = withPlayer { it.transportControls.pause() }
                override fun onStop() = withPlayer { it.transportControls.pause() }
                override fun onSkipToNext() = withPlayer { it.transportControls.skipToNext() }
                override fun onSkipToPrevious() = withPlayer { it.transportControls.skipToPrevious() }
                override fun onSeekTo(pos: Long) = withPlayer { it.transportControls.seekTo(pos) }
            })
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1f)
                    .build()
            )
            // Left inactive on purpose so it never steals the steering-wheel buttons.
        }
        sessionToken = session.sessionToken
        connect()
    }

    /** Run an action on the player's controller, connecting first if needed. */
    private fun withPlayer(action: (MediaControllerCompat) -> Unit) {
        val c = controller
        if (c != null) action(c) else { pending += action; connect() }
    }

    private fun findPlayerService(): ComponentName? {
        val intent = Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE).setPackage(BuildConfig.PLAYER_PACKAGE)
        val info = packageManager.queryIntentServices(intent, 0).firstOrNull() ?: return null
        return ComponentName(info.serviceInfo.packageName, info.serviceInfo.name)
    }

    private fun connect() {
        if (browser != null) return
        val component = findPlayerService()
        if (component == null) {
            Log.w(tag, "No media service found in ${BuildConfig.PLAYER_PACKAGE}")
            retryLater(); return
        }
        Log.i(tag, "Connecting to $component")
        val b = MediaBrowserCompat(this, component, object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                val c = MediaControllerCompat(this@CarMediaTransportService, browser!!.sessionToken)
                controller = c
                c.registerCallback(playerCallback, handler)
                playerCallback.onMetadataChanged(c.metadata)
                playerCallback.onPlaybackStateChanged(c.playbackState)
                Log.i(tag, "Connected to player")
                pending.forEach { it(c) }; pending.clear()
            }
            override fun onConnectionSuspended() { drop(); retryLater() }
            override fun onConnectionFailed() {
                Log.w(tag, "Player refused the connection")
                drop(); retryLater()
            }
        }, null)
        browser = b
        b.connect()
    }

    private fun drop() {
        controller?.unregisterCallback(playerCallback)
        controller = null
        runCatching { browser?.disconnect() }
        browser = null
    }

    private fun retryLater() {
        handler.removeCallbacksAndMessages(RETRY)
        handler.postAtTime({ connect() }, RETRY, android.os.SystemClock.uptimeMillis() + 3000)
    }

    private val playerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            session.setMetadata(metadata?.let { withArtUri(it) })
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            session.setPlaybackState(state)
        }
        override fun onSessionDestroyed() { drop(); retryLater() }
    }

    /** Save the cover to a file and point DISPLAY_ICON_URI at it (BYD reads this URI). */
    private fun withArtUri(m: MediaMetadataCompat): MediaMetadataCompat {
        val art: Bitmap = m.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON)
            ?: return m
        return try {
            val key = listOf(
                m.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                m.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
                m.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            ).joinToString("|")
            val name = "a" + Integer.toHexString(key.hashCode()) + ".png"
            val dir = File(cacheDir, "art").apply { mkdirs() }
            val file = File(dir, name)
            if (!file.exists()) {
                file.outputStream().use { art.compress(Bitmap.CompressFormat.PNG, 100, it) }
                // Keep only the 5 newest covers
                dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
            }
            MediaMetadataCompat.Builder(m)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, "content://$packageName.art/art/$name")
                .build()
        } catch (t: Throwable) {
            Log.w(tag, "Could not save album art", t); m
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?) =
        BrowserRoot("root", null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        drop()
        session.release()
        super.onDestroy()
    }

    private companion object { val RETRY = Any() }
}
