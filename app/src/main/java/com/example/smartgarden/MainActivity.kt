package com.example.smartgarden

import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.smartgarden.data.local.SettingsStorage
import com.example.smartgarden.data.model.GardenDeviceState
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenSettings
import com.example.smartgarden.data.model.GardenStatus
import com.example.smartgarden.data.model.PumpState
import com.example.smartgarden.data.model.SensorSnapshot
import com.example.smartgarden.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class Page { DASHBOARD, SCHEDULE, INSIGHTS, SYSTEM, MANUAL }

    private lateinit var binding: ActivityMainBinding
    private lateinit var pages: Map<Page, View>
    private lateinit var navigationItems: Map<Page, NavigationItem>

    private val settingsStorage by lazy(LazyThreadSafetyMode.NONE) { SettingsStorage(this) }
    private val primaryColor by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getColor(this, R.color.primary_green)
    }
    private val mutedColor by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getColor(this, R.color.text_secondary)
    }
    private val whiteColor by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getColor(this, R.color.white)
    }

    private var currentPage = Page.DASHBOARD
    private var renderedPage: Page? = null
    private var settings = GardenSettings()
    private var deviceState = GardenDeviceState()
    private var simulationIndex = 0

    private var renderedMode: GardenMode? = null
    private var renderedPumpState: PumpState? = null
    private var renderedConnection: Boolean? = null
    private var renderedSoilMoisture: Int? = null
    private var renderedTemperature: Int? = null
    private var renderedAirHumidity: Int? = null
    private var renderedScheduleEnabled: Boolean? = null
    private var renderedScheduleHour: Int? = null
    private var renderedScheduleMinute: Int? = null
    private var renderedThreshold: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewReferences()
        restoreState(savedInstanceState)
        setupNavigation()
        setupModeControls()
        setupSchedule()
        setupInsights()
        setupSystem()
        renderAll()
    }

    private fun setupViewReferences() {
        pages = mapOf(
            Page.DASHBOARD to binding.layoutDashboard,
            Page.SCHEDULE to binding.layoutSchedule,
            Page.INSIGHTS to binding.layoutInsights,
            Page.SYSTEM to binding.layoutSystem,
            Page.MANUAL to binding.layoutManualControl,
        )
        navigationItems = mapOf(
            Page.DASHBOARD to NavigationItem(binding.cardDashboard, binding.iconDashboard, binding.textDashboard),
            Page.SCHEDULE to NavigationItem(binding.cardSchedule, binding.iconSchedule, binding.textSchedule),
            Page.INSIGHTS to NavigationItem(binding.cardInsights, binding.iconInsights, binding.textInsights),
            Page.SYSTEM to NavigationItem(binding.cardSystem, binding.iconSystem, binding.textSystem),
        )
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        currentPage = savedInstanceState?.getString(KEY_PAGE)?.let { saved ->
            Page.entries.firstOrNull { it.name == saved }
        } ?: Page.DASHBOARD
        settings = settingsStorage.load()
        val restoredPumpState = savedInstanceState?.getString(KEY_PUMP_STATE)?.let { saved ->
            PumpState.entries.firstOrNull { it.name == saved }
        } ?: PumpState.OFF
        deviceState = deviceState.copy(mode = settings.mode, pumpState = restoredPumpState)
        applyAutomaticPumpLogic()
    }

    private fun setupNavigation() = with(binding) {
        navDashboard.setOnClickListener { showPage(Page.DASHBOARD) }
        navSchedule.setOnClickListener { showPage(Page.SCHEDULE) }
        navInsights.setOnClickListener { showPage(Page.INSIGHTS) }
        navSystem.setOnClickListener { showPage(Page.SYSTEM) }
        btnSettings.setOnClickListener { showPage(Page.SYSTEM) }
    }

    private fun setupModeControls() = with(binding) {
        btnManual.setOnClickListener { selectMode(GardenMode.MANUAL) }
        btnAutomatic.setOnClickListener { selectMode(GardenMode.AUTO) }
        btnPumpOn.setOnClickListener { setManualPumpState(PumpState.ON) }
        btnPumpOff.setOnClickListener { setManualPumpState(PumpState.OFF) }
    }

    private fun selectMode(mode: GardenMode) {
        if (deviceState.mode != mode) {
            settings = settings.copy(mode = mode)
            deviceState = deviceState.copy(mode = mode)
            settingsStorage.setAutomaticMode(mode == GardenMode.AUTO)
            applyAutomaticPumpLogic()
        }

        showPage(if (mode == GardenMode.MANUAL) Page.MANUAL else Page.DASHBOARD)
        renderMode()
        renderPump()
        if (mode == GardenMode.AUTO) toast(getString(R.string.automatic_mode_active))
    }

    private fun setManualPumpState(pumpState: PumpState) {
        if (deviceState.mode != GardenMode.MANUAL) {
            toast(getString(R.string.switch_to_manual_first))
            return
        }
        if (deviceState.pumpState != pumpState) {
            deviceState = deviceState.copy(pumpState = pumpState)
            renderPump()
        }
        toast(getString(if (pumpState == PumpState.ON) R.string.pump_on else R.string.pump_off))
    }

    private fun setupSchedule() = with(binding) {
        switchSchedule.isChecked = settings.isScheduleEnabled
        seekThreshold.progress = settings.moistureThreshold

        switchSchedule.setOnCheckedChangeListener { _, checked ->
            if (settings.isScheduleEnabled != checked) {
                settings = settings.copy(isScheduleEnabled = checked)
                settingsStorage.setScheduleEnabled(checked)
            }
            renderSchedule()
        }
        seekThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val safeValue = GardenController.clampThreshold(progress)
                if (fromUser && safeValue != progress) {
                    seekBar?.progress = safeValue
                } else {
                    tvThreshold.text = getString(R.string.percentage_value, safeValue)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val value = GardenController.clampThreshold(
                    seekBar?.progress ?: GardenSettings.DEFAULT_MOISTURE_THRESHOLD
                )
                if (settings.moistureThreshold != value) {
                    settings = settings.copy(moistureThreshold = value)
                    settingsStorage.setMoistureThreshold(value)
                    renderedThreshold = null
                    renderSchedule()
                    renderInsights()
                }
            }
        })
        btnScheduleTime.setOnClickListener { openTimePicker() }
    }

    private fun openTimePicker() {
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (settings.scheduleHour != selectedHour || settings.scheduleMinute != selectedMinute) {
                settings = settings.copy(scheduleHour = selectedHour, scheduleMinute = selectedMinute)
                settingsStorage.setScheduleTime(selectedHour, selectedMinute)
                renderSchedule()
            }
        }, settings.scheduleHour, settings.scheduleMinute, true).show()
    }

    private fun setupInsights() {
        binding.btnRefreshInsights.setOnClickListener {
            advanceSimulation()
            toast(getString(R.string.insights_updated))
        }
    }

    private fun setupSystem() = with(binding) {
        switchNotifications.isChecked = settings.areNotificationsEnabled
        switchNotifications.setOnCheckedChangeListener { _, checked ->
            if (settings.areNotificationsEnabled != checked) {
                settings = settings.copy(areNotificationsEnabled = checked)
                settingsStorage.setNotificationsEnabled(checked)
            }
        }
        btnReconnect.setOnClickListener {
            if (!deviceState.isConnected) {
                deviceState = deviceState.copy(isConnected = true)
                renderConnection()
            }
            toast(getString(R.string.connection_successful))
        }
        btnResetSettings.setOnClickListener { resetSettings() }
    }

    private fun resetSettings() = with(binding) {
        settings = settingsStorage.reset()
        deviceState = deviceState.copy(mode = settings.mode, pumpState = PumpState.OFF)
        applyAutomaticPumpLogic()
        switchSchedule.isChecked = settings.isScheduleEnabled
        seekThreshold.progress = settings.moistureThreshold
        switchNotifications.isChecked = settings.areNotificationsEnabled
        invalidateRenderCache()
        renderAll()
        toast(getString(R.string.settings_reset_success))
    }

    private fun invalidateRenderCache() {
        renderedMode = null
        renderedPumpState = null
        renderedConnection = null
        renderedSoilMoisture = null
        renderedTemperature = null
        renderedAirHumidity = null
        renderedScheduleEnabled = null
        renderedScheduleHour = null
        renderedScheduleMinute = null
        renderedThreshold = null
    }

    private fun renderAll() {
        showPage(currentPage)
        renderMode()
        renderPump()
        renderSchedule()
        renderDeviceReadings()
        renderInsights()
        renderConnection()
    }

    private fun showPage(page: Page) {
        if (renderedPage == page) return

        val previousPage = renderedPage
        if (previousPage == null) {
            pages.forEach { (candidate, view) ->
                view.visibility = if (candidate == page) View.VISIBLE else View.GONE
            }
            navigationItems.forEach { (candidate, item) ->
                item.render(candidate == page.navigationPage(), primaryColor, mutedColor, whiteColor)
            }
        } else {
            pages.getValue(previousPage).visibility = View.GONE
            pages.getValue(page).visibility = View.VISIBLE
            val previousNavigation = previousPage.navigationPage()
            val nextNavigation = page.navigationPage()
            if (previousNavigation != nextNavigation) {
                navigationItems.getValue(previousNavigation).render(false, primaryColor, mutedColor, whiteColor)
                navigationItems.getValue(nextNavigation).render(true, primaryColor, mutedColor, whiteColor)
            }
        }
        currentPage = page
        renderedPage = page
    }

    private fun Page.navigationPage(): Page = if (this == Page.MANUAL) Page.DASHBOARD else this

    private fun renderMode() {
        val mode = deviceState.mode
        if (renderedMode == mode) return
        val isManual = mode == GardenMode.MANUAL
        renderSegment(binding.btnAutomatic, binding.tvAutomatic, !isManual)
        renderSegment(binding.btnManual, binding.tvManual, isManual)
        binding.btnPumpOn.isEnabled = isManual
        binding.btnPumpOff.isEnabled = isManual
        renderedMode = mode
    }

    private fun renderSegment(card: CardView, label: TextView, active: Boolean) {
        card.setCardBackgroundColor(if (active) primaryColor else Color.TRANSPARENT)
        label.setTextColor(if (active) whiteColor else mutedColor)
    }

    private fun renderPump() {
        val pumpState = deviceState.pumpState
        if (renderedPumpState == pumpState) return
        val isPumpOn = pumpState == PumpState.ON
        binding.btnPumpOn.setCardBackgroundColor(if (isPumpOn) primaryColor else mutedColor)
        binding.btnPumpOff.setCardBackgroundColor(if (isPumpOn) mutedColor else primaryColor)
        val statusResource = if (isPumpOn) R.string.pump_status_on else R.string.pump_status_off
        binding.tvPumpStatus.setText(statusResource)
        binding.tvPumpStatus.setTextColor(if (isPumpOn) primaryColor else mutedColor)
        binding.tvDashboardPumpStatus.setText(statusResource)
        renderedPumpState = pumpState
    }

    private fun renderSchedule() = with(binding) {
        if (renderedScheduleHour != settings.scheduleHour || renderedScheduleMinute != settings.scheduleMinute) {
            tvScheduleTime.text = String.format(
                Locale.getDefault(), "%02d:%02d", settings.scheduleHour, settings.scheduleMinute
            )
            renderedScheduleHour = settings.scheduleHour
            renderedScheduleMinute = settings.scheduleMinute
        }
        if (renderedThreshold != settings.moistureThreshold) {
            tvThreshold.text = getString(R.string.percentage_value, settings.moistureThreshold)
            renderedThreshold = settings.moistureThreshold
        }
        if (renderedScheduleEnabled != settings.isScheduleEnabled) {
            seekThreshold.isEnabled = settings.isScheduleEnabled
            btnScheduleTime.isEnabled = settings.isScheduleEnabled
            tvScheduleState.setText(
                if (settings.isScheduleEnabled) R.string.schedule_active else R.string.schedule_inactive
            )
            renderedScheduleEnabled = settings.isScheduleEnabled
        }
    }

    private fun renderInsights() = with(binding) {
        tvInsightMoisture.text = getString(R.string.percentage_value, deviceState.soilMoisture)
        tvInsightTemperature.text = getString(R.string.temperature_value, deviceState.temperature)
        tvInsightHumidity.text = getString(R.string.percentage_value, deviceState.airHumidity)
        tvInsightSummary.setText(
            when (GardenController.evaluateGardenStatus(deviceState)) {
                GardenStatus.HEALTHY -> R.string.garden_healthy
                GardenStatus.NEEDS_WATERING -> R.string.garden_needs_watering
                GardenStatus.WARNING -> R.string.garden_warning
            }
        )
        tvInsightUpdated.text = getString(
            R.string.last_updated,
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
        )
    }

    private fun advanceSimulation() {
        simulationIndex = (simulationIndex + 1) % SIMULATED_READINGS.size
        val reading = SIMULATED_READINGS[simulationIndex]
        val nextPumpState = GardenController.evaluateAutoPumpState(
            mode = deviceState.mode,
            soilMoisture = reading.moisture,
            currentPumpState = deviceState.pumpState,
        )
        deviceState = deviceState.copy(
            soilMoisture = reading.moisture,
            temperature = reading.temperature,
            airHumidity = reading.humidity,
            pumpState = nextPumpState,
        )
        renderDeviceReadings()
        renderPump()
        renderInsights()
    }

    private fun applyAutomaticPumpLogic() {
        val nextState = GardenController.evaluateAutoPumpState(
            mode = deviceState.mode,
            soilMoisture = deviceState.soilMoisture,
            currentPumpState = deviceState.pumpState,
        )
        if (nextState != deviceState.pumpState) {
            deviceState = deviceState.copy(pumpState = nextState)
        }
    }

    private fun renderDeviceReadings() = with(binding) {
        if (renderedSoilMoisture != deviceState.soilMoisture) {
            tvDashboardSoilMoisture.text = getString(R.string.percentage_value, deviceState.soilMoisture)
            renderedSoilMoisture = deviceState.soilMoisture
        }
        if (renderedTemperature != deviceState.temperature) {
            tvDashboardTemperature.text = getString(R.string.temperature_value, deviceState.temperature)
            renderedTemperature = deviceState.temperature
        }
        if (renderedAirHumidity != deviceState.airHumidity) {
            tvDashboardAirHumidity.text = getString(R.string.percentage_value, deviceState.airHumidity)
            renderedAirHumidity = deviceState.airHumidity
        }
    }

    private fun renderConnection() {
        if (renderedConnection == deviceState.isConnected) return
        val statusResource = if (deviceState.isConnected) R.string.connected else R.string.disconnected
        binding.tvDashboardConnection.setText(statusResource)
        binding.tvConnectionStatus.setText(statusResource)
        renderedConnection = deviceState.isConnected
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_PAGE, currentPage.name)
        outState.putString(KEY_PUMP_STATE, deviceState.pumpState.name)
        super.onSaveInstanceState(outState)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private data class NavigationItem(val card: CardView, val icon: ImageView, val label: TextView) {
        fun render(active: Boolean, primary: Int, muted: Int, white: Int) {
            card.setCardBackgroundColor(if (active) primary else Color.TRANSPARENT)
            icon.setColorFilter(if (active) white else muted)
            label.setTextColor(if (active) primary else muted)
            label.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    companion object {
        private const val KEY_PAGE = "current_page"
        private const val KEY_PUMP_STATE = "pump_state"
        private val SIMULATED_READINGS = listOf(
            SensorSnapshot(moisture = 65, temperature = 24, humidity = 48),
            SensorSnapshot(moisture = 38, temperature = 25, humidity = 50),
            SensorSnapshot(moisture = 42, temperature = 25, humidity = 51),
            SensorSnapshot(moisture = 46, temperature = 24, humidity = 49),
            SensorSnapshot(moisture = 64, temperature = 40, humidity = 47),
        )
    }
}
