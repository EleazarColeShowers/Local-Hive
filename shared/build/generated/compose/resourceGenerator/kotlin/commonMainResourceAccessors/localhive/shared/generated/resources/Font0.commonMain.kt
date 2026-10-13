@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package localhive.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainFont0 {
  public val nunito_bold: FontResource by 
      lazy { init_nunito_bold() }

  public val nunito_extrabold: FontResource by 
      lazy { init_nunito_extrabold() }

  public val nunito_medium: FontResource by 
      lazy { init_nunito_medium() }

  public val nunito_regular: FontResource by 
      lazy { init_nunito_regular() }

  public val nunito_semibold: FontResource by 
      lazy { init_nunito_semibold() }
}

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("nunito_bold", CommonMainFont0.nunito_bold)
  map.put("nunito_extrabold", CommonMainFont0.nunito_extrabold)
  map.put("nunito_medium", CommonMainFont0.nunito_medium)
  map.put("nunito_regular", CommonMainFont0.nunito_regular)
  map.put("nunito_semibold", CommonMainFont0.nunito_semibold)
}

internal val Res.font.nunito_bold: FontResource
  get() = CommonMainFont0.nunito_bold

private fun init_nunito_bold(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:nunito_bold",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/font/nunito_bold.ttf", -1, -1),
    )
)

internal val Res.font.nunito_extrabold: FontResource
  get() = CommonMainFont0.nunito_extrabold

private fun init_nunito_extrabold(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:nunito_extrabold",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/font/nunito_extrabold.ttf", -1, -1),
    )
)

internal val Res.font.nunito_medium: FontResource
  get() = CommonMainFont0.nunito_medium

private fun init_nunito_medium(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:nunito_medium",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/font/nunito_medium.ttf", -1, -1),
    )
)

internal val Res.font.nunito_regular: FontResource
  get() = CommonMainFont0.nunito_regular

private fun init_nunito_regular(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:nunito_regular",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/font/nunito_regular.ttf", -1, -1),
    )
)

internal val Res.font.nunito_semibold: FontResource
  get() = CommonMainFont0.nunito_semibold

private fun init_nunito_semibold(): FontResource = org.jetbrains.compose.resources.FontResource(
  "font:nunito_semibold",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/localhive.shared.generated.resources/font/nunito_semibold.ttf", -1, -1),
    )
)
