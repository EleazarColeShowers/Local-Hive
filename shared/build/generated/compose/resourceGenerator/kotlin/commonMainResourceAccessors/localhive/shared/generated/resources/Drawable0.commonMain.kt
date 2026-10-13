@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package localhive.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainDrawable0 {
  public val carouselone: DrawableResource by 
      lazy { init_carouselone() }

  public val carouselthree: DrawableResource by 
      lazy { init_carouselthree() }

  public val carouseltwo: DrawableResource by 
      lazy { init_carouseltwo() }

  public val compose_multiplatform: DrawableResource by 
      lazy { init_compose_multiplatform() }

  public val localhivelogo: DrawableResource by 
      lazy { init_localhivelogo() }
}

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("carouselone", CommonMainDrawable0.carouselone)
  map.put("carouselthree", CommonMainDrawable0.carouselthree)
  map.put("carouseltwo", CommonMainDrawable0.carouseltwo)
  map.put("compose_multiplatform", CommonMainDrawable0.compose_multiplatform)
  map.put("localhivelogo", CommonMainDrawable0.localhivelogo)
}

internal val Res.drawable.carouselone: DrawableResource
  get() = CommonMainDrawable0.carouselone

private fun init_carouselone(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:carouselone",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/drawable/carouselone.xml", -1, -1),
    )
)

internal val Res.drawable.carouselthree: DrawableResource
  get() = CommonMainDrawable0.carouselthree

private fun init_carouselthree(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:carouselthree",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/drawable/carouselthree.xml", -1, -1),
    )
)

internal val Res.drawable.carouseltwo: DrawableResource
  get() = CommonMainDrawable0.carouseltwo

private fun init_carouseltwo(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:carouseltwo",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/drawable/carouseltwo.xml", -1, -1),
    )
)

internal val Res.drawable.compose_multiplatform: DrawableResource
  get() = CommonMainDrawable0.compose_multiplatform

private fun init_compose_multiplatform(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:compose_multiplatform",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/drawable/compose-multiplatform.xml", -1, -1),
    )
)

internal val Res.drawable.localhivelogo: DrawableResource
  get() = CommonMainDrawable0.localhivelogo

private fun init_localhivelogo(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:localhivelogo",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/drawable/localhivelogo.png", -1, -1),
    )
)
