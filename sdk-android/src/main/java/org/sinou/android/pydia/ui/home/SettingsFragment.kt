package org.sinou.android.pydia.ui.home

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.sinou.android.pydia.R

/** Display XML based settings */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
