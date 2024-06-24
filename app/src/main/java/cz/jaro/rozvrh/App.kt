package cz.jaro.rozvrh

import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            defaultModule()
            modules(module {
                single {
                    contentResolver
                }
                single {
                    getSharedPreferences("PREFS_Rozvrh_Manual", Context.MODE_PRIVATE)
                }
                single {
                    filesDir.toPath()
                }
            })
        }
    }
}
