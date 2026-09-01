package threeway.henroute.orchard

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import threeway.henroute.orchard.core.navigation.openMenu
import threeway.henroute.orchard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureOrientationByDestination()
        configureBackNavigation()
    }

    private fun configureOrientationByDestination() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            requestedOrientation = when (destination.id) {
                R.id.splashFragment,
                R.id.onBoardingGreyFragment,
                R.id.noInternetGreyFragment -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun configureBackNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when (navController.currentDestination?.id) {
                        R.id.menuFragment -> finish()
                        R.id.levelsFragment,
                        R.id.settingsFragment,
                        R.id.leadersFragment,
                        R.id.achievementsFragment,
                        R.id.shopFragment,
                        R.id.gameFragment,
                        R.id.resultFragment -> navController.openMenu()
                        else -> navController.navigateUp()
                    }
                }
            }
        )
    }

    override fun onStop() {
        (application as App).serviceLocator.soundManager.pauseMusic()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        (application as App).serviceLocator.soundManager.resumeMusic()
    }
}
