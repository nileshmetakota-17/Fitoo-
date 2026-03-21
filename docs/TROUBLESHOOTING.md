# Troubleshooting

## Gradle sync fails / SDK not found

- Ensure Android Studio has the Android SDK installed.
- Confirm you have a valid `local.properties` pointing at the SDK location.
- Re-sync Gradle and try again.

## Gradle says “Android Gradle plugin requires Java 17”

Set Android Studio / Gradle to use **JDK 17**.

## App won’t run on device

- `minSdk = 26` → requires Android 8.0+.
- Check you selected a compatible emulator/device.

## Sensei says “AI request failed”

If you configured an OpenAI key:

- Verify the key is valid and has quota.
- Check the selected model name stored in prefs (`openai_model`).
- Ensure the device/emulator has network access (INTERNET permission is included).

If you did not configure a key:

- No-key backend availability depends on the external provider.
- Try switching models in the Sensei model picker and retry.

## My data disappeared after an update

The app uses Room with `fallbackToDestructiveMigration()`. Schema version bumps can wipe local data.

## Reset everything

Use Profile → “Delete all app data” to clear:

- `fitoo.db`
- `fitoo_prefs`
- cached/files (including the avatar)

