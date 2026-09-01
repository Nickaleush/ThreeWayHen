package threeway.henroute.orchard.feature.shop

import threeway.henroute.orchard.R

object ShopArtwork {
    fun previewFor(id: String): Int = when (id) {
        "hen_preview" -> R.drawable.skin_blue
        "hen_quick" -> R.drawable.skin_green
        "hen_magnet" -> R.drawable.skin_red
        "hen_auto" -> R.drawable.skin_yellow
        else -> R.drawable.skin_default
    }
}
