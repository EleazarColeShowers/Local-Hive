package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.theme.BackgroundGold
import localhive.shared.generated.resources.Res
import localhive.shared.generated.resources.localhivelogo
import org.jetbrains.compose.resources.painterResource

/** Bare splash: the logo on brand gold. Where to go next is decided by the host's route. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGold),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.localhivelogo),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )
    }
}
