package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.R

@Composable
fun EstateSelectionScreen(onEstateJoined: () -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateEstateScreen(onSuccess = onEstateJoined, onBack = { showCreate = false })
        return
    }
    if (showJoin) {
        JoinEstateScreen(onSuccess = onEstateJoined, onBack = { showJoin = false })
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.backgroundgold))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Find Your Estate",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.darkgray),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Create a new estate community or join an existing one",
                fontSize = 15.sp, color = colorResource(id = R.color.darkgray).copy(alpha = 0.7f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 48.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showCreate = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.hivegreen)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Create Estate", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Start a new community for your estate", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showJoin = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = colorResource(id = R.color.hivegreen), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Join Estate", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colorResource(id = R.color.darkgray))
                        Text("Enter an invite code to join", fontSize = 13.sp, color = colorResource(id = R.color.darkgray).copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
