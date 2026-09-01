package threeway.henroute.orchard.core.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import threeway.henroute.orchard.R

object AppFonts {
    fun black(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.inter_18pt_black) ?: Typeface.DEFAULT_BOLD

    fun apply(view: TextView) {
        view.typeface = black(view.context)
    }

    fun applyToTree(view: View) {
        if (view is TextView) apply(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                applyToTree(view.getChildAt(index))
            }
        }
    }
}
