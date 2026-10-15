# Open and run

1. Extract the ZIP.
2. Open the extracted root folder in Android Studio.
3. Let Android Studio create `local.properties`. If it does not, copy `local.properties.example` to `local.properties` and set your own Android SDK path.
4. Run **Sync Project with Gradle Files**.
5. Run **Build > Clean Project** and **Build > Rebuild Project**.
6. Run the `app` configuration on an emulator or Android device.

## Required functional checks

- Register, log in, change password, log out and log in with the new password.
- Open Category → Science and test text question, gallery and camera.
- Test Science with network unavailable to confirm the `OFFLINE MODE` answer.
- Create two accounts and confirm Math History/Notes do not appear across accounts.
- Open Programming directly from Category and verify its dashboard appears.

Gemini API key values/configuration were kept as supplied, following the requested scope.
