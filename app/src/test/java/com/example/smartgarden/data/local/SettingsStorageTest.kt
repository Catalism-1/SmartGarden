package com.example.smartgarden.data.local

import android.content.SharedPreferences
import com.example.smartgarden.data.model.AppConnectionMode
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStorageTest {
    private val preferences = InMemorySharedPreferences()
    private val storage = SettingsStorage(preferences)

    @Test
    fun emptyStorage_returnsDefaultSettings() {
        assertEquals(GardenSettings(), storage.load())
    }

    @Test
    fun savedSettings_areLoadedTogether() {
        storage.setAutomaticMode(false)
        storage.setConnectionMode(AppConnectionMode.LOCAL_SERVER)
        storage.setLocalServerIp("192.168.1.10")
        storage.setScheduleEnabled(false)
        storage.setScheduleTime(18, 30)
        storage.setMoistureThreshold(55)
        storage.setStopMoistureThreshold(70)
        storage.setPumpSafety(maxDurationSeconds = 20, cooldownSeconds = 90)
        storage.setNotificationsEnabled(false)

        assertEquals(
            GardenSettings(
                mode = GardenMode.MANUAL,
                connectionMode = AppConnectionMode.LOCAL_SERVER,
                localServerIp = "192.168.1.10",
                isScheduleEnabled = false,
                scheduleHour = 18,
                scheduleMinute = 30,
                moistureThreshold = 55,
                stopMoistureThreshold = 70,
                maxPumpDurationSeconds = 20,
                cooldownSeconds = 90,
                areNotificationsEnabled = false,
            ),
            storage.load(),
        )
    }

    @Test
    fun reset_clearsSavedSettings() {
        storage.setAutomaticMode(false)
        storage.setMoistureThreshold(70)

        assertEquals(GardenSettings(), storage.reset())
        assertEquals(GardenSettings(), storage.load())
    }

    @Test
    fun moistureThreshold_isPersistedWithinSafeRange() {
        storage.setMoistureThreshold(5)
        assertEquals(40, storage.load().moistureThreshold)

        storage.setMoistureThreshold(100)
        assertEquals(90, storage.load().moistureThreshold)
    }

    @Test
    fun localServerSettings_arePersisted() {
        storage.setConnectionMode(AppConnectionMode.LOCAL_SERVER)
        storage.setLocalServerIp(" 192.168.10.20 ")

        assertEquals(AppConnectionMode.LOCAL_SERVER, storage.load().connectionMode)
        assertEquals("192.168.10.20", storage.load().localServerIp)
    }

    @Test
    fun pumpSafetySettings_areClamped() {
        storage.setStopMoistureThreshold(20)
        storage.setPumpSafety(maxDurationSeconds = -5, cooldownSeconds = -10)

        assertEquals(41, storage.load().stopMoistureThreshold)
        assertEquals(1, storage.load().maxPumpDurationSeconds)
        assertEquals(0, storage.load().cooldownSeconds)
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST") ((values[key] as? Set<String>) ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor(values)

    private class Editor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)
        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = put(key, values)
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) removals += key
        }

        override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }
        override fun commit(): Boolean {
            applyChanges()
            return true
        }
        override fun apply() = applyChanges()

        private fun put(key: String?, value: Any?): SharedPreferences.Editor = apply {
            if (key != null) changes[key] = value
        }

        private fun applyChanges() {
            if (clearRequested) values.clear()
            removals.forEach(values::remove)
            changes.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
        }
    }
}
