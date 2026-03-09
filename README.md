# NewsCheck Android App
**Developer:** Jhun Lawrence Del Rosario

## Run the App
1. Clone and checkout this branch:
```
   git clone https://github.com/neoboi76/newscheck.git
   cd newscheck
   git checkout frontend-RenceJC
```
2. Open the `NewsCheck` folder in Android Studio
3. Start the backend: `docker-compose up -d`
4. Hit ▶ Run — app is ready!

> Using emulator? Already configured. ✅  
> Using physical device? Change `BASE_URL` in `app/build.gradle` to your PC's local IP.

---

## Optional: Enable FCM Push Notifications
1. Go to [Firebase Console](https://console.firebase.google.com) → project **newscheck-3dd55**
2. Download `google-services.json` → place it in `app/` folder
3. Follow FCM setup instructions in the code comments
