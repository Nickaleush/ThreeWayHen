package threeway.henroute.orchard.core.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import threeway.henroute.orchard.R

/** Draws a rounded dark outline around bright/white game text. */
open class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val outlineColor = context.getColor(R.color.onboarding_text_outline)
    private val outlineWidth = resources.displayMetrics.density * 2.2f
    private var drawingPasses = false

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        includeFontPadding = false
    }

    private fun shouldOutline(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (r > 225 && g > 225 && b > 225) || (r > 230 && g > 150 && b < 100)
    }

    override fun invalidate() {
        if (!drawingPasses) super.invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val fillColor = currentTextColor
        if (!shouldOutline(fillColor)) {
            super.onDraw(canvas)
            return
        }

        drawingPasses = true
        val oldStyle = paint.style
        val oldWidth = paint.strokeWidth
        val oldJoin = paint.strokeJoin

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outlineWidth
        paint.strokeJoin = Paint.Join.ROUND
        setTextColor(outlineColor)
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        setTextColor(fillColor)
        super.onDraw(canvas)

        paint.style = oldStyle
        paint.strokeWidth = oldWidth
        paint.strokeJoin = oldJoin
        setTextColor(fillColor)
        drawingPasses = false
    }
}
