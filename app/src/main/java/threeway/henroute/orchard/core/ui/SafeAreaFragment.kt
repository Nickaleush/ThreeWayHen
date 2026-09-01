package threeway.henroute.orchard.core.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Fragment base that keeps interactive content out of status bars, navigation bars,
 * display cutouts, and (when requested) system gesture regions.
 *
 * Layout backgrounds may still fill the fragment bounds, while the fragment root's
 * original padding is preserved and increased by the current safe-area insets.
 */
abstract class SafeAreaFragment(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

    /** Enable for screens whose primary interaction uses edge swipes, such as the game. */
    protected open val protectSystemGestures: Boolean = false

    /** Some full-screen landscape overlays intentionally draw without safe-area padding. */
    protected open val applySafeArea: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (applySafeArea) {
            view.applySafeAreaInsets(includeSystemGestures = protectSystemGestures)
        }
        AppFonts.applyToTree(view)
    }
}

fun View.applySafeAreaInsets(includeSystemGestures: Boolean = false) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { target, windowInsets ->
        var insetTypes = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout()
        if (includeSystemGestures) {
            insetTypes = insetTypes or WindowInsetsCompat.Type.systemGestures()
        }

        val safeInsets = windowInsets.getInsets(insetTypes)
        target.updatePadding(
            left = initialLeft + safeInsets.left,
            top = initialTop + safeInsets.top,
            right = initialRight + safeInsets.right,
            bottom = initialBottom + safeInsets.bottom
        )

        // Keep dispatching the same insets. Child views can opt into more specific
        // handling without losing the window information.
        windowInsets
    }

    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        doOnAttach { ViewCompat.requestApplyInsets(it) }
    }
}
