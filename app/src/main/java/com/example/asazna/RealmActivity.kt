package com.example.asazna

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmActivity:Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)  //初期化

        val config = RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .allowWritesOnUiThread(true)
            .allowQueriesOnUiThread(true)
            .build()

        Realm.setDefaultConfiguration(config)
    }
}