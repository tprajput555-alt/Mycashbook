# My Cashbook (Android)

English-language cashbook app:
- Cash In / Cash Out
- Online In / Online Out
- Net balance and category totals
- Transaction notes, timestamps, and delete
- Local SQLite storage (works offline)

## Build APK automatically with GitHub Actions
1. Create a new GitHub repository and upload the contents of this folder.
2. Open the repository's **Actions** tab.
3. Select **Build Android APK** and click **Run workflow** (or push to `main`).
4. When the workflow completes, open its run and download the `MyCashbook-debug-apk` artifact.
5. Extract the downloaded ZIP to get `app-debug.apk`, then install it on your Android phone.

## Build locally with Android Studio
Open this folder in Android Studio, allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

This environment does not have the Android SDK/Gradle build environment, so the APK has not been compiled here.
