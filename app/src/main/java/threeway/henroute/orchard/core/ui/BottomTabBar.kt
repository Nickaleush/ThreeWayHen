package threeway.henroute.orchard.core.ui

import android.view.View
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import threeway.henroute.orchard.R

object BottomTabBar {
    enum class Tab { Levels, Settings, Leaders, Achievements, Shop }

    fun setup(root: View, navController: NavController, activeTab: Tab) {
        val tabs = listOf(
            Triple(R.id.tabLevelsButton, Tab.Levels, R.id.levelsFragment),
            Triple(R.id.tabSettingsButton, Tab.Settings, R.id.settingsFragment),
            Triple(R.id.tabLeadersButton, Tab.Leaders, R.id.leadersFragment),
            Triple(R.id.tabAchievementsButton, Tab.Achievements, R.id.achievementsFragment),
            Triple(R.id.tabShopButton, Tab.Shop, R.id.shopFragment)
        )
        tabs.forEach { (viewId, tab, destination) ->
            root.findViewById<ImageView?>(viewId)?.apply {
                val active = tab == activeTab
                alpha = if (active) 1f else 0.62f
                scaleX = if (active) 1f else 0.9f
                scaleY = if (active) 1f else 0.9f
                setOnClickListener {
                    if (navController.currentDestination?.id != destination) {
                        navController.navigate(
                            destination,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.menuFragment, false)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                }
            }
        }
    }
}
