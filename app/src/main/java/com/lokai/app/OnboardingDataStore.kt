package com.lokai.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import java.io.File

val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

// Application-scoped singleton — safe to call from MainActivity AND OnboardingViewModel.
// Using PreferenceDataStoreFactory instead of the preferencesDataStore delegate
// avoids the duplicate-registration IllegalStateException.
object OnboardingDataStore {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun get(context: Context): DataStore<Preferences> =
        instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create {
                File(context.applicationContext.filesDir, "datastore/onboarding.preferences_pb")
            }.also { instance = it }
        }
}
