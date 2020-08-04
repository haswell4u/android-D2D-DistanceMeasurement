# 1. Introduction

When researching in the field of mobile systems, depending on the research topic, it is sometimes necessary to measure the distance among mobile devices.
**This application is designed for experiments measuring distances among Android devices** in such cases.
It can measure the distances using modules such as [WiFi RTT][] over [WiFi Aware][], or get RSSI offered by [BLE][].
(You need to convert the RSSI to distance information by applying the specific formula you want to test.)

# 2. Prerequisites

- Android 9 (API level 28) or later.

When the application starts, it checks whether each module is available, and modules not supported by the device are automatically disabled.

It has been confirmed that all modules work successfully in Google Pixel 3.

# 3. Getting Start

First, allow necessary permissions to use implemented modules, and set the device ID.

***IMPORTANT***: All device IDs must be unique. (The function to automatically filter duplicate IDs is not available in this version.)

## 3.1 Main Screen

There are two display parts and three buttons on the main screen.

***Display Parts***
- **Devices** - displays the measured distance or RSSI information from the discovered device.
- **Events** - displays all events that occur during operation. (e.g., device discovery, distance measurement success/failure)

***Buttons***
- **INIT** - initializes the selected modules (can be selected in the settings screen) before distance measurement.
- **START** - starts distance measurement via selected modules. This operation runs as a foreground service - i.e., the measure is possible regardless of the screen off.
- **STOP** - stops all modules and measurement.

Each Button is activated/deactivated depending on the situation.

You can start the measurement process by pressing the INIT button of all devices and the START button.
However, before the beginning, You should select modules (methods) that are supposed to use in the settings screens. (All modules are disabled by default, so nothing is happening right now.)

## 3.2 Settings Screen

At the top right, press the button in the action bar to open the settings screen.
You can select modules you want to run in the Measurement Methods section.

After that, go back to the main screen and initialize and start for measurement. Now distance or RSSI information will be displayed on the Devices screen with events.

See below for other detailed settings.

***Options***
- **General**
  - `Your ID` - this ID will be used to identify the device.
  - `Device Removal Time (ms)`: if the discovered device is not updated for the set time, the device is removed from the list.
- **Measurement Methods**
  - `Use WiFi Aware` - enables WiFi Aware. When WiFi Aware is used alone, only the existence of the discovered device is continuously checked.
  - `Use WiFi RTT` - enables WiFi RTT. This module is only active when WiFi aware is enabled.
  - `Use BLE` - enables BLE.
- **Wifi Aware**
  - `Refresh Interval` - periodically checks for the existence of the discovered device at a set time interval.
- **Wifi RTT**
  - `Measurement Interval` - sets the distance measurement interval. Do not set the time interval too short.
- **Logging**
  - `Write Events to File` - records all generated events in a file. Default file name is [current_time.txt] and location is /Android/data/com.example.distancemeasurement/files/Documents/Events/[current_time.txt].
  - `Use Specific File Names` - You can set a specific file name.
  - `File Name`: You don't need to specify a file extension such as ".txt". The default name is "SNQz4YoYRL.txt".
  
# To Do

- Survey of RSSI-Distance formulas
- Issue of duplication of device IDs
 
[WiFi RTT]: https://developer.android.com/guide/topics/connectivity/wifi-rtt
[WiFi Aware]: https://developer.android.com/guide/topics/connectivity/wifi-aware
[BLE]: https://developer.android.com/guide/topics/connectivity/bluetooth-le
