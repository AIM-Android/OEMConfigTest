# OEMConfig

```bash
adb install app/build/outputs/apk/debug/OEMConfigTest_v1.0.1_debug.apk
```

```bash
adb shell dpm set-device-owner com.advantech.oemconfigtest/.DeviceAdminReceiver
```

```bash
adb shell dpm remove-active-admin com.advantech.oemconfigtest/.DeviceAdminReceiver
```

![](https://github.com/AIM-Android/OEMConfigTest/blob/advantech/doc/oemconfig.png)