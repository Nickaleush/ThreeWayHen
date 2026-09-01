package threeway.henroute.orchard.games.hen

enum class HenAbility {
    None,
    PreviewFar,
    QuickSwipe,
    FeatherMagnet,
    AutoJump
}

data class HenSkin(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val ability: HenAbility
)

object HenSkins {
    val all = listOf(
        HenSkin("hen_classic", "Basic Runner", "Classic chicken with no special ability.", 100, HenAbility.None),
        HenSkin("hen_preview", "Preview Far", "Route hints appear earlier.", 200, HenAbility.PreviewFar),
        HenSkin("hen_quick", "Quick Swipe", "Choose a route later.", 300, HenAbility.QuickSwipe),
        HenSkin("hen_magnet", "Feather Magnet", "Pulls nearby feathers.", 400, HenAbility.FeatherMagnet),
        HenSkin("hen_auto", "Auto Jump", "Jumps over nearby obstacles.", 500, HenAbility.AutoJump)
    )

    fun abilityFor(id: String): HenAbility = all.firstOrNull { it.id == id }?.ability ?: HenAbility.None
}
