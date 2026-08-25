package com.devscope.demo

import android.app.Application
import androidx.room.Room
import com.devscope.DevScope
import com.devscope.Trigger
import com.devscope.demo.crash.FirestoreCrashSink
import com.devscope.demo.data.AppDatabase
import okhttp3.OkHttpClient
import timber.log.Timber

class DemoApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var httpClient: OkHttpClient
        private set

    override fun onCreate() {
        super.onCreate()

        // Regular Timber setup — DevScope's tree composes with this one.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        database = Room.databaseBuilder(this, AppDatabase::class.java, "demo.db").build()

        // The one-line install from the idea document (plus what we track).
        // In a release build this whole block is a no-op inside the library.
        DevScope.install(this)
            .trackDatabase(database, name = "demo.db")
            // Crashes recorded on this device are pushed to Firebase Firestore
            // on the next launch, so they are visible even when the device isn't.
            .uploadCrashesTo(FirestoreCrashSink())
            .openOn(Trigger.SHAKE)

        // Interceptor must be added when the client is built (OkHttp clients
        // are immutable) — so the client is created after DevScope.install.
        httpClient = OkHttpClient.Builder()
            .addInterceptor(DevScope.networkInterceptor)
            .build()

        Timber.i("DevScope demo started")
    }
}
