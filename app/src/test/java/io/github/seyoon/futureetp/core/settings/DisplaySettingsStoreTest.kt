package io.github.seyoon.futureetp.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DisplaySettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** DataStore 하나를 열어 [block]을 돌린 뒤 닫는다. 같은 파일을 두 DataStore가 동시에 열 수 없다 */
    private suspend fun <T> withStore(
        file: File,
        region: String?,
        block: suspend (DisplaySettingsStore) -> T,
    ): T {
        val job = Job()
        val dataStore =
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        return try {
            block(DisplaySettingsStore(dataStore) { region })
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun newFile() = File(folder.newFolder(), "display_settings.preferences_pb")

    @Test
    fun defaultFollowsDeviceRegion() {
        assertEquals(UpDownStyle.KR, defaultDisplaySettings("KR").upDownStyle)
        assertEquals(UpDownStyle.US, defaultDisplaySettings("US").upDownStyle)
        assertEquals(UpDownStyle.US, defaultDisplaySettings("JP").upDownStyle)
        assertEquals(UpDownStyle.US, defaultDisplaySettings(null).upDownStyle)
    }

    @Test
    fun firstDefaultIsKeptWhenRegionChanges() =
        runTest {
            val file = newFile()
            assertEquals(UpDownStyle.KR, withStore(file, "KR") { it.settings.first().upDownStyle })
            assertEquals(UpDownStyle.KR, withStore(file, "US") { it.settings.first().upDownStyle })
        }

    @Test
    fun chosenStyleIsSavedAndEmitted() =
        runTest {
            val file = newFile()
            withStore(file, "KR") { store ->
                store.setUpDownStyle(UpDownStyle.US)
                assertEquals(UpDownStyle.US, store.settings.first().upDownStyle)
            }
            assertEquals(UpDownStyle.US, withStore(file, "KR") { it.settings.first().upDownStyle })
        }

    @Test
    fun unknownStoredValueFallsBackToDefault() =
        runTest {
            val file = newFile()
            withStore(file, "KR") { store ->
                store.setUpDownStyle(UpDownStyle.US)
            }
            // 손상·이전 형식 값은 기본값으로 읽는다
            val job = Job()
            val raw = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
            raw.updateData {
                it.toMutablePreferences().apply {
                    set(
                        androidx.datastore.preferences.core
                            .stringPreferencesKey("upDownStyle"),
                        "JP",
                    )
                }
            }
            job.cancelAndJoin()
            assertEquals(UpDownStyle.KR, withStore(file, "KR") { it.settings.first().upDownStyle })
        }
}
