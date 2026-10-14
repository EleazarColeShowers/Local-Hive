package com.eleazar.localhive.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import localhive.shared.generated.resources.Res
import localhive.shared.generated.resources.carouselone
import localhive.shared.generated.resources.carouselthree
import localhive.shared.generated.resources.carouseltwo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private class OnboardingPage(val image: DrawableResource, val title: String, val description: String)

private val Pages = listOf(
    OnboardingPage(
        Res.drawable.carouselone,
        "Your neighbourhood, in one place",
        "Meet the people in your estate, chat directly, and lend each other a hand."
    ),
    OnboardingPage(
        Res.drawable.carouseltwo,
        "Never miss what's happening",
        "Announcements, events and notices from your estate, as soon as they're posted."
    ),
    OnboardingPage(
        Res.drawable.carouselthree,
        "Join your estate",
        "Find your estate or join with an invite code. Starting a new one takes a minute."
    )
)

/** Three-page intro: an illustration, a title and a line of text each, then sign up. */
@Composable
fun OnboardingScreen(
    onNavigateNext: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == Pages.lastIndex
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isLastPage) {
                TextButton(onClick = onNavigateNext) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = Pages[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(page.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    page.title,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.description,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(Pages.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 20.dp else 8.dp, label = "dotWidth")
                val color by animateColorAsState(
                    if (selected) accent else MaterialTheme.colorScheme.outline,
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Button(
            onClick = {
                if (isLastPage) onNavigateNext()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isLastPage) "Get started" else "Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
