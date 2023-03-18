package com.lablabla.blablawatering.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lablabla.blablawatering.data.repository.Callbacks
import com.lablabla.blablawatering.databinding.ActivityConnectionBinding
import com.lablabla.blablawatering.model.Event
import com.lablabla.blablawatering.model.Station
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.BufferedReader
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionActivity : AppCompatActivity(), Callbacks {

    lateinit var binding: ActivityConnectionBinding

    @Inject
    lateinit var btManager: com.lablabla.blablawatering.bluetooth.BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = ContextCompat.getSystemService(
            this,
            BluetoothManager::class.java
        ) as BluetoothManager
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
//            shouldShowRequestPermissionRationale(...) -> {
//            // In an educational UI, explain to the user why your app requires this
//            // permission for a specific feature to behave as expected. In this UI,
//            // include a "cancel" or "no thanks" button that allows the user to
//            // continue using your app without granting the permission.
//            showInContextUI(...)
//        }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        bluetoothManager.adapter
    }


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                btManager.startScan()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    private fun readAndSetTimezones() : Map<String, String> {
        val timezonesInfo = mutableMapOf<String, String>()
        assets
            .open("zones.csv")
            .bufferedReader()
            .use(BufferedReader::readLines)
            .forEach { s ->
                s.split("\",\"")
                    .also { p ->
                        val id = p[0].replace("\"", "")
                        val str = p[1].replace("\"", "")
                        timezonesInfo[id] = str
                    }
            }
        Timber.d("Parsed timezone file. Found ${timezonesInfo.size}")
        return timezonesInfo
    }

    private var activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.e("On Activity Result OK")
            // TODO: Check if it's ACTION_REQUEST_ENABLE and if so, update own BT manager
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        binding = ActivityConnectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        val timezones = readAndSetTimezones()
        btManager.timezonesMap = timezones
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(enableBtIntent);
        }
        else
        {
            btManager.bluetoothAdapter = bluetoothAdapter
        }
        btManager.callbacks = this
        btManager.startScan()
    }

    override fun onDeviceConnected(name: String, address: String) {
        Timber.d("Connected to device $name at $address")
        Timber.uprootAll()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDeviceDisconnected(name: String, address: String) {
    }

    override fun onStationStateNotification(stations: List<Station>) {
    }

    override fun onUpdateStations(stations: List<Station>) {
    }

    override fun onUpdateEvents(events: List<Event>) {
    }
}