package com.lablabla.blablawatering.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.lablabla.blablawatering.model.Station
import com.lablabla.blablawatering.model.TimeMessage
import com.lablabla.blablawatering.util.fromJson
import com.lablabla.blablawatering.util.isReadable
import com.lablabla.blablawatering.util.isWritable
import timber.log.Timber
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

private const val GATT_MAX_MTU_SIZE = 517
private const val GATT_MIN_MTU_SIZE = 23

@SuppressLint("MissingPermission")
class BluetoothManager @Inject constructor(
    private val context: Context
) {

    private var scanning = false
    private var bluetoothGatt: BluetoothGatt? = null
    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private lateinit var bluetoothDevice: BluetoothDevice
    private var pendingOperation: BleOperationType? = null

    private val ADVERTISING_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")

    private val SERVICE_UUID = UUID.fromString("260bb0ea-6e32-4f94-adf6-b96ebda4c6ce")
    private val GET_STATIONS_UUID = UUID.fromString("1001b0ea-6e32-4f94-adf6-b96ebda4c6ce")
    private val SET_TIME_UUID = UUID.fromString("1005b0ea-6e32-4f94-adf6-b96ebda4c6ce")

    var btCallback: BlablaBTCallback? = null
    var connected: Boolean = false
    var timezonesMap: Map<String, String>? = null

    lateinit var bluetoothAdapter: BluetoothAdapter
    var mtu = GATT_MAX_MTU_SIZE

    val gson = Gson()

    private var stationsString: String = ""

    init {
        listenToBondStateChanges()
    }

    //region BLE Scanning
    private val bleScanner: BluetoothLeScanner? by lazy {
        if (this::bluetoothAdapter.isInitialized) {
            return@lazy bluetoothAdapter.bluetoothLeScanner
        }
        null
    }

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(ADVERTISING_UUID))
        .build()

    @RequiresApi(Build.VERSION_CODES.M)
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        .setReportDelay(0)
        .build()


    fun startScan() {
        if (!scanning) {
            scanning = true
            bleScanner?.startScan(mutableListOf(scanFilter), scanSettings, leScanCallback)
        }
    }

    fun stopScan() {
        if (scanning) {
            scanning = false
            bleScanner?.stopScan(leScanCallback)
        }
    }

    fun requestMtuFromDevice(mtu: Int) {
        if (connected) {
            enqueueOperation(MtuRequest(bluetoothDevice, mtu.coerceIn(GATT_MIN_MTU_SIZE, GATT_MAX_MTU_SIZE)))
        } else {
            Timber.e("Not connected to ${bluetoothDevice.address}, cannot request MTU update!")
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            Timber.e( "Scanning BLE devices found device named $name")
            stopScan()
            bluetoothDevice = result.device
            connect(result.device, context)

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e( "Scanning BLE devices failed with error code $errorCode")
            stopScan()
        }
    }

    fun getDeviceName(): String? {
        return bluetoothGatt?.device?.name
    }

    fun getDeviceAddress(): String? {
        return bluetoothGatt?.device?.address
    }

    fun sync() {
        Timber.d("syncing")
        bluetoothGatt?.apply {
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                device.createBond()
            }
            else {
                //updateTime()
                getStations()
            }
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceName = gatt.device.name
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.w("Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.w("Successfully disconnected from $deviceAddress")
                    teardownConnection(gatt.device)

                    connected = false
                    btCallback?.onDeviceDisconnected(deviceName, deviceAddress)

                }
            } else {
                Timber.w("Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.w("Discovered ${services.size} services for ${device.address}.")
                    printGattTable()
                    connected = true
                    requestMtuFromDevice(GATT_MAX_MTU_SIZE)
                    btCallback?.onDeviceConnected(gatt.device.name, gatt.device.address)
                } else {
                    Timber.e("Service discovery failed due to status $status")
                    teardownConnection(gatt.device)
                }
            }

            if (pendingOperation is Connect) {
                signalEndOfOperation()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Timber.w("ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
            if (pendingOperation is MtuRequest) {
                signalEndOfOperation()
            }
            this@BluetoothManager.mtu = mtu
            sync()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        val utf8String = value.decodeToString()
                        Timber.d( "onWrite $utf8String")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Timber.e("Read not permitted for $uuid!")
                    }
                    else -> {
                        Timber.e("Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicWrite) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Timber.d("Read characteristic $uuid:\n${value.decodeToString()}")
                        when (uuid) {
                            GET_STATIONS_UUID -> {
                                val utf8String = value.decodeToString()
                                stationsString += utf8String
                                if (stationsString.endsWith("\n")) {
                                    val stations: List<Station> = gson.fromJson(stationsString)
                                    btCallback?.onUpdateStations(stations)
                                }
                                else {
                                    readCharacteristic(this)
                                }
                            }
                            else -> {
                                Timber.e("Unknown UUID $uuid")
                            }
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Timber.e("Read not permitted for $uuid!")
                    }
                    else -> {
                        Timber.e("Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicRead) {
                signalEndOfOperation()
            }
        }
    }

    private fun listenToBondStateChanges() {
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            with(intent) {
                if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val previousBondState = getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val bondState = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val bondTransition = "${previousBondState.toBondStateDescription()} to " +
                            bondState.toBondStateDescription()
                    Timber.w("${device?.address} bond state changed | $bondTransition")
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        sync()
                    }
                }

            }
        }

        private fun Int.toBondStateDescription() = when(this) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_NONE -> "NOT BONDED"
            else -> "ERROR: $this"
        }
    }

    private fun sendToCharacteristic(characteristic: BluetoothGattCharacteristic, value: String) {
        // Remove all newline characters and add \n at the end
        sendToCharacteristicInternal(characteristic, "$$value".replace("\n", "") + '\n')
    }

    private fun sendToCharacteristicInternal(characteristic: BluetoothGattCharacteristic, value: String)
    {
        if (value.length > mtu)
        {
            sendToCharacteristicInternal(characteristic, value.substring(0, mtu))
            sendToCharacteristicInternal(characteristic, value.substring(mtu))
        }
        else
        {
            writeCharacteristic(characteristic, value.toByteArray())
        }
    }

    private fun updateTime() {
        val calendar = GregorianCalendar()
        val ts = Instant.now().epochSecond
        val timeZone = timezonesMap?.get(calendar.timeZone.id) ?: ""

        val timeMessage = TimeMessage(ts, 0, timeZone)
        val setTimeChar = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(SET_TIME_UUID)
        if (setTimeChar?.isWritable() == true) {
            val timeStr = gson.toJson(timeMessage)
            Timber.d("setting time to $timeStr")
            sendToCharacteristic(setTimeChar, timeStr)
        }

    }

    private fun getStations() {
        Timber.d("getting stations")
        stationsString = ""
        bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(GET_STATIONS_UUID)
            ?.let { getStationsChar ->
                readCharacteristic(getStationsChar)
        }
    }

    @Synchronized
    private fun enqueueOperation(operation: BleOperationType) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun signalEndOfOperation() {
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    /**
     * Perform a given [BleOperationType]. All permission checks are performed before an operation
     * can be enqueued by [enqueueOperation].
     */
    @Synchronized
    private fun doNextOperation() {
        if (pendingOperation != null) return

        val operation = operationQueue.poll() ?: run {
            Timber.v("Operation queue empty, returning")
            return
        }
        pendingOperation = operation

        // Handle Connect separately from other operations that require device to be connected
        if (operation is Connect) {
            with(operation) {
                Timber.w("Connecting to ${device.address}")
                device.connectGatt(context, false, gattCallback)
            }
            return
        }

        // Check BluetoothGatt availability for other operations
        val gatt = bluetoothGatt
            ?: this@BluetoothManager.run {
                Timber.e("Not connected to ${operation.device.address}! Aborting $operation operation.")
                signalEndOfOperation()
                return
            }

        // TODO: Make sure each operation ultimately leads to signalEndOfOperation()
        // TODO: Refactor this into an BleOperationType abstract or extension function
        when (operation) {
            is Disconnect -> with(operation) {
                Timber.w("Disconnecting from ${device.address}")
                gatt.close()
                signalEndOfOperation()
            }
            is CharacteristicWrite -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    characteristic.writeType = writeType
                    characteristic.value = payload
                    gatt.writeCharacteristic(characteristic)
                } ?: this@BluetoothManager.run {
                    Timber.e("Cannot find $characteristicUuid to write to")
                    signalEndOfOperation()
                }
            }
            is CharacteristicRead -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    gatt.readCharacteristic(characteristic)
                } ?: this@BluetoothManager.run {
                    Timber.e("Cannot find $characteristicUuid to read from")
                    signalEndOfOperation()
                }
            }
            is DescriptorWrite -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    descriptor.value = payload
                    gatt.writeDescriptor(descriptor)
                } ?: this@BluetoothManager.run {
                    Timber.e("Cannot find $descriptorUuid to write to")
                    signalEndOfOperation()
                }
            }
            is DescriptorRead -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    gatt.readDescriptor(descriptor)
                } ?: this@BluetoothManager.run {
                    Timber.e("Cannot find $descriptorUuid to read from")
                    signalEndOfOperation()
                }
            }
            is MtuRequest -> with(operation) {
                gatt.requestMtu(mtu)
            }
        }
    }


    fun connect(device: BluetoothDevice, context: Context) {
        if (connected) {
            Timber.e("Already connected to ${device.address}!")
        } else {
            enqueueOperation(Connect(device, context.applicationContext))
        }
    }

    fun teardownConnection(device: BluetoothDevice) {
        if (connected) {
            enqueueOperation(Disconnect(device))
        } else {
            Timber.e("Not connected to ${device.address}, cannot teardown connection!")
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (connected && characteristic.isReadable()) {
            enqueueOperation(CharacteristicRead(bluetoothDevice, characteristic.uuid))
        } else if (!characteristic.isReadable()) {
            Timber.e("Attempting to read ${characteristic.uuid} that isn't readable!")
        } else if (!connected) {
            Timber.e("Not connected to ${bluetoothDevice.address}, cannot perform characteristic read")
        }
    }

    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray
    ) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> {
                Timber.e("Characteristic ${characteristic.uuid} cannot be written to")
                return
            }
        }
        if (connected) {
            enqueueOperation(CharacteristicWrite(bluetoothDevice, characteristic.uuid, writeType, payload))
        } else {
            Timber.e("Not connected to ${bluetoothDevice.address}, cannot perform characteristic write")
        }
    }
}