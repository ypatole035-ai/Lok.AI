package com.lokai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.ui.components.DeviceProfileCard
import com.lokai.app.viewmodel.DeviceViewModel

private val BgPage      = Color(0xFF0D0D0D)
private val AccentAmber = Color(0xFFF5A623)
private val TextSubtle  = Color(0xFF666666)

@Composable
fun DeviceScreen(vm: DeviceViewModel = viewModel()) {
    val profile   by vm.profile.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = BgPage) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Text(
                text      = "Your Device",
                color     = AccentAmber,
                fontSize  = 22.sp,
                fontWeight= FontWeight.Bold
            )
            Text(
                text    = "Hardware detected at launch · refresh to update",
                color   = TextSubtle,
                fontSize= 12.sp
            )
            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentAmber)
                    }
                }

                profile == null -> {
                    Text(
                        text    = "Could not read device hardware.",
                        color   = Color(0xFFEF9A9A),
                        fontSize= 14.sp
                    )
                }

                else -> {
                    DeviceProfileCard(profile = profile!!)

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { vm.refresh() },
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextSubtle)
                    ) {
                        Text("Refresh hardware scan", fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
