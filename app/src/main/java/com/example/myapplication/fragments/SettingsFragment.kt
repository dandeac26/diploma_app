package com.example.myapplication.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.config.ConfigManager

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val configManager = ConfigManager(requireContext())

        val mobileUrlPreference = findPreference<EditTextPreference>("baseUrlMobile")
        mobileUrlPreference?.setOnPreferenceChangeListener { _, newValue ->
            configManager.baseUrlMobile = newValue as String
            Log.d("SettingsFragment", "baseUrlMobile: ${configManager.baseUrlMobile}, useHomeUrl: ${configManager.useHomeUrl}")
            true
        }

        val homeUrlPreference = findPreference<EditTextPreference>("baseUrlHome")
        homeUrlPreference?.setOnPreferenceChangeListener { _, newValue ->
            configManager.baseUrlHome = newValue as String
            Log.d("SettingsFragment", "baseUrlHome: ${configManager.baseUrlHome}, useHomeUrl: ${configManager.useHomeUrl}")
            true
        }

        val useHomeUrlPreference = findPreference<SwitchPreferenceCompat>("useHomeUrl")
        useHomeUrlPreference?.setOnPreferenceChangeListener { _, newValue ->
            configManager.useHomeUrl = newValue as Boolean
            Log.d("SettingsFragment", "baseUrlHome: ${configManager.baseUrlHome}, useHomeUrl: ${configManager.useHomeUrl}")
            true
        }

        val restartAppPreference = findPreference<Preference>("restartApp")
        restartAppPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        }

        val closeSettingsPreference = findPreference<Preference>("closeSettings")
        closeSettingsPreference?.setOnPreferenceClickListener {
            parentFragmentManager.popBackStack()
            true
        }
    }
}