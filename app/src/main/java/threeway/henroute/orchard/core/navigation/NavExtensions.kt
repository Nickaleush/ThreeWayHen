package threeway.henroute.orchard.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import threeway.henroute.orchard.R

fun NavController.openMenu() {
    if (currentDestination?.id == R.id.menuFragment) return
    navigate(
        R.id.menuFragment,
        null,
        NavOptions.Builder()
            .setPopUpTo(R.id.menuFragment, false)
            .setLaunchSingleTop(true)
            .build()
    )
}
