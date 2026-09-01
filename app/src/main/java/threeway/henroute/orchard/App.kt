package threeway.henroute.orchard

import android.app.Application
import threeway.henroute.orchard.core.AppServiceLocator

class App : Application() {

    lateinit var serviceLocator: AppServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = AppServiceLocator(this)
    }

    override fun onTerminate() {
        serviceLocator.soundManager.release()
        super.onTerminate()
    }
}
