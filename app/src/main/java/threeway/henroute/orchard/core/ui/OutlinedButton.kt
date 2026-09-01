package threeway.henroute.orchard.core.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import threeway.henroute.orchard.R

/** Button counterpart of OutlinedTextView for white game labels. */
class OutlinedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {
    private val outlineColor = context.getColor(R.color.onboarding_text_outline)
    private val outlineWidth = resources.displayMetrics.density * 2.2f
    private var drawingPasses = false

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    private fun shouldOutline(color: Int): Boolean =
        Color.red(color) > 225 && Color.green(color) > 225 && Color.blue(color) > 225

    override fun invalidate() { if (!drawingPasses) super.invalidate() }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val fillColor = currentTextColor
        if (!shouldOutline(fillColor)) return super.onDraw(canvas)
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
