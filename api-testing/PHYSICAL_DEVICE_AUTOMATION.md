# SoulMatch Physical Device Automation

Use this when a real Android phone is connected over USB.

## One-time Phone Setup

1. Enable Developer options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Accept the Allow USB debugging popup on the phone.
5. Confirm the device is visible:

```powershell
adb devices
```

Expected:

```text
<device_id>    device
```

## Run Smoke Automation

From the SoulMatch repo root:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\run-android-physical-smoke.ps1
```

To skip reinstalling the app and test the currently installed build:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\run-android-physical-smoke.ps1 -SkipInstall
```

## What It Tests

- ADB can see the device.
- Debug APK installs.
- SoulMatch launches.
- Login screen is visible.
- Log In opens Mobile verification.
- Mobile number input accepts a dummy value.
- Send OTP becomes visible/enabled.
- Crash log remains empty.

The script does not tap Send OTP, does not clear app data, and does not perform destructive actions.

## Output

Results are written to:

```text
api-testing/device-smoke/
```

This folder is intentionally ignored by Git because it contains screenshots and local test artifacts.
