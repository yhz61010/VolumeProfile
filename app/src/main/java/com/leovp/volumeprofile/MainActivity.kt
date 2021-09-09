package com.leovp.volumeprofile

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast

class MainActivity : Activity() {
    private val pref: SharedPreferences by lazy { getSharedPreferences("leo_volume_profile", MODE_PRIVATE) }

    private val audioManager: AudioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val ringPlayer: Ringtone by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) } // TYPE_RINGTONE
    private val alarmPlayer: Ringtone? by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }
    private val musicPlayer: Ringtone by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }

    private val maxNotificationVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) }
    private val maxRingVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) }
    private val maxAlarmVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) }
    private val maxMusicVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    private val maxVoiceCallVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) }
    private val maxSystemVolume: Int by lazy { audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) }

    companion object {
        private const val MODE_CUSTOM = 1
        private const val MODE_OUTDOOR = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize shortcut action in dynamic way
//        initShortCutAction()

        when (intent.action) {
            "action_clear_pref" -> {
                clearPref()
                Toast.makeText(this, "Preference cleared!", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            "action_outdoor" -> {
                switchToOutdoorMode()
                finish()
                return
            }
            "action_custom" -> {
                switchToCustomMode()
                finish()
                return
            }
        }

        if (pref.getInt("conf", -1) > 0) {
            when (pref.getInt("mode", MODE_CUSTOM)) {
                MODE_CUSTOM -> switchToOutdoorMode()
                MODE_OUTDOOR -> switchToCustomMode()
                else -> Toast.makeText(this, "Unknown mode", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        initView()

        findViewById<SeekBar>(R.id.seekBarRing).progress = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        findViewById<SeekBar>(R.id.seekBarRing).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setIntPref("volume_ring", seekBar.progress)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopAllSound()
                audioManager.setStreamVolume(AudioManager.STREAM_RING, seekBar.progress, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, seekBar.progress, 0)
                ringPlayer.play()
            }
        })

        findViewById<SeekBar>(R.id.seekBarAlarm).progress = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        findViewById<SeekBar>(R.id.seekBarAlarm).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setIntPref("volume_alarm", seekBar.progress)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, findViewById<SeekBar>(R.id.seekBarRing).progress, 0)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.e("LEO", "alarm volume: $progress")
                // Set alarm volume to system
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0)
                // On my Honor V20 - HarmonyOS 2.0.0, if you just [AudioManager.STREAM_NOTIFICATION] is same with [AudioManager.STREAM_RING]
                // Play the alarm in proper volume with either [AudioManager.STREAM_NOTIFICATION] or [AudioManager.STREAM_RING]
                audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopAllSound()
                // Set alarm volume to system
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, seekBar.progress, 0)
                // On my Honor V20 - HarmonyOS 2.0.0, if you just [AudioManager.STREAM_NOTIFICATION] is same with [AudioManager.STREAM_RING]
                // Play the alarm in proper volume with either [AudioManager.STREAM_NOTIFICATION] or [AudioManager.STREAM_RING]
                audioManager.setStreamVolume(AudioManager.STREAM_RING, seekBar.progress, 0)
                alarmPlayer?.play()
            }
        })

        findViewById<SeekBar>(R.id.seekBarMusic).progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        findViewById<SeekBar>(R.id.seekBarMusic).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setIntPref("volume_music", seekBar.progress)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, findViewById<SeekBar>(R.id.seekBarRing).progress, 0)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Set music volume to system
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                // On my Honor V20 - HarmonyOS 2.0.0, if you just [AudioManager.STREAM_NOTIFICATION] is same with [AudioManager.STREAM_RING]
                // Play the music in proper volume with either [AudioManager.STREAM_NOTIFICATION] or [AudioManager.STREAM_RING]
                audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopAllSound()
                // Set music volume to system
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.progress, 0)
                // On my Honor V20 - HarmonyOS 2.0.0, if you just [AudioManager.STREAM_NOTIFICATION] is same with [AudioManager.STREAM_RING]
                // Play the music in proper volume with either [AudioManager.STREAM_NOTIFICATION] or [AudioManager.STREAM_RING]
                audioManager.setStreamVolume(AudioManager.STREAM_RING, seekBar.progress, 0)
                musicPlayer.play()
            }
        })
    }

    private fun switchToCustomMode() {
        enableLowerIcon()
        Toast.makeText(this, "Custom mode", Toast.LENGTH_SHORT).show()
        setIntPref("mode", MODE_CUSTOM)
        switchToSpecificVolumeMode()
    }

    private fun switchToOutdoorMode() {
        enableLouderIcon()
        Toast.makeText(this, "Outdoor mode", Toast.LENGTH_SHORT).show()
        setIntPref("mode", MODE_OUTDOOR)
        setAllSoundToMaxVolume()
    }

//    private fun initShortCutAction() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.action = "action_clear_pref"
//        val shortcut = ShortcutInfoCompat.Builder(this, "clear_pref")
//            .setShortLabel(getString(R.string.shortcut_short_label))
//            .setLongLabel(getString(R.string.shortcut_long_label))
//            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_clear))
//            .setIntent(intent)
//            .build()
//
//        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
//    }

    private fun switchToSpecificVolumeMode() {
        audioManager.setStreamVolume(AudioManager.STREAM_RING, getIntPref("volume_ring", 1), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, getIntPref("volume_alarm", 1), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getIntPref("volume_music", 1), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, getIntPref("volume_ring", 1), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoiceCallVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, getIntPref("volume_ring", 1), 0)
    }

    private fun setAllSoundToMaxVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoiceCallVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxSystemVolume, 0)
    }

    private fun setAllVolumeBySeekBar() {
        audioManager.setStreamVolume(AudioManager.STREAM_RING, findViewById<SeekBar>(R.id.seekBarRing).progress, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, findViewById<SeekBar>(R.id.seekBarAlarm).progress, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, findViewById<SeekBar>(R.id.seekBarMusic).progress, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, findViewById<SeekBar>(R.id.seekBarRing).progress, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoiceCallVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, findViewById<SeekBar>(R.id.seekBarRing).progress, 0)

        setIntPref("volume_ring", findViewById<SeekBar>(R.id.seekBarRing).progress)
        setIntPref("volume_alarm", findViewById<SeekBar>(R.id.seekBarAlarm).progress)
        setIntPref("volume_music", findViewById<SeekBar>(R.id.seekBarMusic).progress)
    }

    private fun initView() {
        findViewById<SeekBar>(R.id.seekBarRing).max = maxRingVolume
        findViewById<SeekBar>(R.id.seekBarAlarm).max = maxAlarmVolume
        findViewById<SeekBar>(R.id.seekBarMusic).max = maxMusicVolume
    }

    private fun stopAllSound() {
        alarmPlayer?.stop()
        musicPlayer.stop()
        ringPlayer.stop()
    }

    override fun onPause() {
        stopAllSound()
        super.onPause()
    }

    fun onConfirmClick(@Suppress("UNUSED_PARAMETER") view: View) {
        stopAllSound()
        setIntPref("conf", 1)
        setAllVolumeBySeekBar()
        enableLowerIcon()
        finish()
    }

    private fun setIntPref(key: String, value: Int) {
        pref.edit().apply {
            putInt(key, value)
            apply()
        }
    }

    @Suppress("SameParameterValue")
    private fun getIntPref(key: String, defaultValue: Int): Int = pref.getInt(key, defaultValue)

    private fun enableLouderIcon() {
        packageManager.setComponentEnabledSetting(ComponentName(this, "com.leovp.volumeprofile.MainActivityLouder"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(ComponentName(this, "com.leovp.volumeprofile.MainActivityLower"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }

    private fun enableLowerIcon() {
        packageManager.setComponentEnabledSetting(ComponentName(this, "com.leovp.volumeprofile.MainActivityLower"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(ComponentName(this, "com.leovp.volumeprofile.MainActivityLouder"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }

    private fun clearPref() {
        pref.edit().clear().apply()
    }
}