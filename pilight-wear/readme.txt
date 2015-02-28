Start Emaulator with Android Wear profile and connect with the following command physical handheld with wear emulator:
adb -d forward tcp:5601 tcp:5601


Bluetooth debugging
adb forward tcp:4444 localabstract:/adb-hub
adb connect localhost:4444