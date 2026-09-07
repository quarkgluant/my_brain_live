Initial project setup and implementation of My Brain Live EEG monitoring app (via 3 different devices: Bitalino kit, MindWave mobile 2 and BainLink Lite V2.0)

- Set up Android project structure using Jetpack Compose, Kotlin, Material3, and Gradle version catalog.
- Configure AndroidManifest.xml with Bluetooth (Classic and LE) and Location permissions, FileProvider, and MainActivity configuration.
- Implement `EegBluetoothManager` supporting SPP RFCOMM connections and BLE GATT notification services for PLUX, NeuroSky MindWave Mobile, and BrainLink Lite devices.
- Add `NeuroSkyParser` and `PluxFrame` parsers to decode ThinkGear packets, eSense attention/meditation metrics, raw waveform data, and ASIC power bands.
- Create `EegDashboardScreen` Compose UI featuring Bluetooth device scanning/filtering, live telemetry monitoring, real-time waveform oscilloscope, attention/meditation time-series charts, and frequency spectrum visualization.
- Implement `EegExporter` and `EegViewModel` recording session logic to support exporting telemetry data in HDF5 (.h5), CSV, and JSON formats with MediaStore integration and file sharing.
