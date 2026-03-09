# FCM Push Notifications Setup

## Requirements
- Access to Firebase project **newscheck-3dd55**
- `google-services.json` from Firebase Console

## Steps
1. Go to [Firebase Console](https://console.firebase.google.com) → project **newscheck-3dd55**
2. Click ⚙️ **Project Settings** → **Your apps** → Android app
3. Download `google-services.json` → place it inside the `app/` folder:
```
   NewsCheck/
   └── app/
       └── google-services.json  ← place here
```
4. Add to root `build.gradle` plugins:
```groovy
   id 'com.google.gms.google-services' version '4.4.0' apply false
```
5. Add to `app/build.gradle` plugins:
```groovy
   id 'com.google.gms.google-services'
```
6. Add to `app/build.gradle` dependencies:
```groovy
   implementation 'com.google.firebase:firebase-messaging:23.4.0'
```
7. Create `NewsCheckMessagingService.kt` inside `com.newscheck.app`:
```kotlin
   package com.newscheck.app

   import com.google.firebase.messaging.FirebaseMessagingService
   import com.google.firebase.messaging.RemoteMessage
   import android.util.Log

   class NewsCheckMessagingService : FirebaseMessagingService() {
       override fun onMessageReceived(message: RemoteMessage) {
           Log.d("FCM", "Message received: ${message.notification?.title}")
       }
       override fun onNewToken(token: String) {
           Log.d("FCM", "New token: $token")
       }
   }
```
8. Add to `AndroidManifest.xml` inside `<application>`:
```xml
   <service
       android:name=".NewsCheckMessagingService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
```
9. Rebuild and run!

> ⚠️ **Note:** Without `google-services.json` the app will **fail to build** entirely.
> Only add the FCM dependencies in `build.gradle` after placing the file in `app/` first.