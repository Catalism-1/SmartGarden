package com.example.smartgarden

import com.example.smartgarden.data.model.GardenDeviceState
import com.example.smartgarden.data.model.GardenMode
import com.example.smartgarden.data.model.GardenStatus
import com.example.smartgarden.data.model.PumpState
import org.junit.Assert.assertEquals
import org.junit.Test

class GardenControllerTest {
    @Test
    fun threshold_isClampedToSupportedRange() {
        assertEquals(40, GardenController.clampThreshold(-5))
        assertEquals(45, GardenController.clampThreshold(45))
        assertEquals(90, GardenController.clampThreshold(120))
    }

    @Test
    fun autoMode_soilBelow40_turnsPumpOn() {
        val result = GardenController.evaluateAutoPumpState(
            mode = GardenMode.AUTO,
            soilMoisture = 39,
            currentPumpState = PumpState.OFF,
        )

        assertEquals(PumpState.ON, result)
    }

    @Test
    fun autoMode_soilAtLeast55_turnsPumpOff() {
        val result = GardenController.evaluateAutoPumpState(
            mode = GardenMode.AUTO,
            soilMoisture = 55,
            currentPumpState = PumpState.ON,
        )

        assertEquals(PumpState.OFF, result)
    }

    @Test
    fun autoMode_hysteresisBand_keepsCurrentPumpState() {
        assertEquals(
            PumpState.ON,
            GardenController.evaluateAutoPumpState(GardenMode.AUTO, 42, PumpState.ON),
        )
        assertEquals(
            PumpState.OFF,
            GardenController.evaluateAutoPumpState(GardenMode.AUTO, 42, PumpState.OFF),
        )
    }

    @Test
    fun manualMode_doesNotChangePumpAutomatically() {
        assertEquals(
            PumpState.OFF,
            GardenController.evaluateAutoPumpState(GardenMode.MANUAL, 20, PumpState.OFF),
        )
        assertEquals(
            PumpState.ON,
            GardenController.evaluateAutoPumpState(GardenMode.MANUAL, 80, PumpState.ON),
        )
    }

    @Test
    fun extremeTemperature_returnsWarning() {
        val state = GardenDeviceState(
            soilMoisture = 65,
            temperature = 42,
            airHumidity = 48,
        )

        assertEquals(GardenStatus.WARNING, GardenController.evaluateGardenStatus(state))
    }

    @Test
    fun extremeAirHumidity_returnsWarning() {
        val state = GardenDeviceState(
            soilMoisture = 65,
            temperature = 24,
            airHumidity = 90,
        )

        assertEquals(GardenStatus.WARNING, GardenController.evaluateGardenStatus(state))
    }

    @Test
    fun normalReadings_areHealthy() {
        val state = GardenDeviceState(
            soilMoisture = 65,
            temperature = 24,
            airHumidity = 48,
        )

        assertEquals(GardenStatus.HEALTHY, GardenController.evaluateGardenStatus(state))
    }
}
