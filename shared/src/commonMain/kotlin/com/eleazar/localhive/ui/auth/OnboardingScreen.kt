package com.eleazar.localhive.ui.auth

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import localhive.shared.generated.resources.Res
import localhive.shared.generated.resources.carouselone
import localhive.shared.generated.resources.carouselthree
import localhive.shared.generated.resources.carouseltwo
import localhive.shared.generated.resources.localhivelogo
import com.eleazar.localhive.ui.theme.BackgroundGold
import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.theme.HoneyGold
import com.eleazar.localhive.ui.theme.LightGold
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onNavigateNext: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            image = Res.drawable.carouselone,
            eyebrow = "Your neighbourhood, online",
            title = "Welcome to ",
            highlight = "LocalHive",
            description = "Connect with your neighbours and build a stronger community together",
            features = listOf(
                Icons.Filled.Home to "Meet neighbours",
                Icons.Filled.Email to "Chat directly",
                Icons.Filled.Favorite to "Lend a hand"
            ),
            topChip = Icons.Filled.Person to "New neighbour joined",
            bottomChip = Icons.Filled.Star to "Your hive is buzzing"
        ),
        OnboardingPage(
            image = Res.drawable.carouseltwo,
            eyebrow = "Never out of the loop",
            title = "Stay ",
            highlight = "Up To Date",
            description = "Never miss important updates, events, or announcements in your estate",
            features = listOf(
                Icons.Filled.Info to "Announcements",
                Icons.Filled.DateRange to "Events",
                Icons.Filled.Notifications to "Alerts"
            ),
            topChip = Icons.Filled.DateRange to "Estate meeting at 6pm",
            bottomChip = Icons.Filled.Notifications to "3 new updates"
        ),
        OnboardingPage(
            image = Res.drawable.carouselthree,
            eyebrow = "Ready when you are",
            title = "Join Your ",
            highlight = "Community",
            description = "Find your estate or create a new one to get started",
            features = listOf(
                Icons.Filled.Search to "Find estate",
                Icons.Filled.Lock to "Join by code",
                Icons.Filled.Add to "Start new"
            ),
            topChip = Icons.Filled.CheckCircle to "Invite code accepted",
            bottomChip = Icons.Filled.Home to "Welcome home!"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val hiveGreen = HiveGreen
    val honeyGold = HoneyGold
    val darkGray = DarkGray
    val lightGold = LightGold
    val backgroundGold = BackgroundGold

    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lightGold, backgroundGold)))
    ) {
        HoneycombBackdrop(color = honeyGold.copy(alpha = 0.18f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Brand bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.localhivelogo),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "LocalHive",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGray,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                )
                if (!isLastPage) {
                    Text(
                        text = "Skip",
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { onNavigateNext() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = darkGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    hiveGreen = hiveGreen,
                    honeyGold = honeyGold,
                    darkGray = darkGray
                )
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dotWidth")
                        val color by animateColorAsState(
                            if (selected) hiveGreen else hiveGreen.copy(alpha = 0.25f),
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onNavigateNext()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = hiveGreen),
                    colors = ButtonDefaults.buttonColors(containerColor = hiveGreen),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ctaLabel"
                    ) { last ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (last) "Get Started" else "Next",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    hiveGreen: Color,
    honeyGold: Color,
    darkGray: Color
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Shrink the hero on short screens so the text never gets pushed off.
        val compact = maxHeight < 560.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            HeroCard(
                page = page,
                honeyGold = honeyGold,
                darkGray = darkGray,
                maxHeroHeight = if (compact) 200.dp else 300.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = page.eyebrow.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = hiveGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(hiveGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        append(page.title)
                        withStyle(SpanStyle(color = hiveGreen)) { append(page.highlight) }
                    },
                    fontSize = if (compact) 26.sp else 30.sp,
                    lineHeight = if (compact) 32.sp else 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = darkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
                )

                Text(
                    text = page.description,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = darkGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                page.features.forEach { (icon, label) ->
                    FeatureTile(
                        icon = icon,
                        label = label,
                        hiveGreen = hiveGreen,
                        darkGray = darkGray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    page: OnboardingPage,
    honeyGold: Color,
    darkGray: Color,
    maxHeroHeight: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeroHeight + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Warm glow behind the card
        Box(
            modifier = Modifier
                .size(maxHeroHeight)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(honeyGold.copy(alpha = 0.45f), Color.Transparent))
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeroHeight)
                .padding(horizontal = 4.dp)
                .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = darkGray.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(1.dp, honeyGold.copy(alpha = 0.25f), RoundedCornerShape(32.dp))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        FloatingChip(
            chip = page.topChip,
            honeyGold = honeyGold,
            darkGray = darkGray,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-4).dp)
        )
        FloatingChip(
            chip = page.bottomChip,
            honeyGold = honeyGold,
            darkGray = darkGray,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp)
        )
    }
}

@Composable
private fun FloatingChip(
    chip: Pair<ImageVector, String>,
    honeyGold: Color,
    darkGray: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(honeyGold),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = chip.first,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = chip.second,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = darkGray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun FeatureTile(
    icon: ImageVector,
    label: String,
    hiveGreen: Color,
    darkGray: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(hiveGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = hiveGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = darkGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Faint honeycomb outlines scattered in the corners to give the background texture. */
@Composable
private fun HoneycombBackdrop(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = 28.dp.toPx()
        val stroke = Stroke(width = 1.5.dp.toPx())

        fun hexagon(center: Offset) = Path().apply {
            for (i in 0 until 6) {
                val angle = (60.0 * i - 30.0) * PI / 180.0
                val point = Offset(
                    center.x + radius * cos(angle).toFloat(),
                    center.y + radius * sin(angle).toFloat()
                )
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }

        val colStep = radius * 1.732f
        val rowStep = radius * 1.5f

        // Top-right cluster
        listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 1 to 1, 1 to 2).forEach { (c, r) ->
            val x = size.width - colStep * c - (if (r % 2 == 1) colStep / 2 else 0f)
            drawPath(hexagon(Offset(x, rowStep * r + radius * 2)), color, style = stroke)
        }
        // Bottom-left cluster
        listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 1 to 1, 0 to 2).forEach { (c, r) ->
            val x = colStep * c + (if (r % 2 == 1) colStep / 2 else 0f)
            drawPath(hexagon(Offset(x, size.height - rowStep * r - radius * 3)), color, style = stroke)
        }
    }
}

data class OnboardingPage(
    val image: DrawableResource,
    val eyebrow: String,
    val title: String,
    val highlight: String,
    val description: String,
    val features: List<Pair<ImageVector, String>>,
    val topChip: Pair<ImageVector, String>,
    val bottomChip: Pair<ImageVector, String>
)
