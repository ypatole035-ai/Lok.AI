package com.lokai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lokai.app.model.DownloadState
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant
import com.lokai.app.ui.components.*
import com.lokai.app.viewmodel.DownloadViewModel
import com.lokai.app.viewmodel.ModelViewModel

private val BgPage      = Color(0xFF0D0D0D)
private val AccentAmber = Color(0xFFF5A623)
private val TextSubtle  = Color(0xFF666666)

@Composable
fun ModelBrowserScreen(
    modelVm:    ModelViewModel    = viewModel(),
    downloadVm: DownloadViewModel = viewModel(),
    onHFSearch: () -> Unit        = {}
) {
    val result     by modelVm.result.collectAsStateWithLifecycle()
    val isLoading  by modelVm.isLoading.collectAsStateWithLifecycle()
    val downloadUi by downloadVm.uiState.collectAsStateWithLifecycle()

    // Confirm sheet state
    var pendingDownload by remember { mutableStateOf<Pair<ModelEntry, ModelVariant>?>(null) }

    // Delete dialog
    val deleteModelId = downloadUi.deleteConfirmModelId
    if (deleteModelId != null) {
        val name = downloadUi.downloadedModels.firstOrNull { it.modelId == deleteModelId }?.name ?: deleteModelId
        DeleteModelDialog(
            modelName = name,
            onConfirm = { downloadVm.confirmDelete(deleteModelId) },
            onDismiss = { downloadVm.dismissDeleteConfirm() }
        )
    }

    // Download confirm bottom sheet
    pendingDownload?.let { (model, variant) ->
        DownloadConfirmSheet(
            model          = model,
            variant        = variant,
            availableRamGb = result?.deviceRamGb ?: 4f,
            onConfirm      = {
                downloadVm.startDownload(model)
                pendingDownload = null
            },
            onDismiss = { pendingDownload = null }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgPage) {
        when {
            isLoading  -> BrowserLoading()
            result == null -> BrowserError()
            else -> {
                val data = result!!
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column {
                            Text("Models", color = AccentAmber, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Filtered for your ${"%.1f".format(data.deviceRamGb)} GB effective RAM",
                                color = TextSubtle, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onHFSearch,
                                border  = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                                shape   = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔍  Search HuggingFace", color = AccentAmber, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    item {
                        BrowserSectionHeader(
                            title    = "✓ Compatible",
                            subtitle = "${data.compatible.size} models will run on your device",
                            color    = Color(0xFF81C784)
                        )
                    }

                    if (data.compatible.isEmpty()) {
                        item {
                            Text(
                                "No compatible models found. Your device may have very limited RAM.",
                                color = TextSubtle, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(data.compatible, key = { it.id }) { model ->
                            val dlState by downloadVm.downloadStateFor(model.id)
                                .collectAsStateWithLifecycle()

                            when (dlState) {
                                is DownloadState.Queued,
                                is DownloadState.Downloading,
                                is DownloadState.Verifying -> {
                                    DownloadProgressCard(
                                        modelName = model.name,
                                        quant     = model.bestVariantFor(data.deviceRamGb)?.quant ?: "",
                                        state     = dlState,
                                        onCancel  = { downloadVm.cancelDownload(model.id) }
                                    )
                                }
                                else -> {
                                    ModelCard(
                                        model          = model,
                                        availableRamGb = data.deviceRamGb,
                                        compatible     = true,
                                        downloadState  = dlState,
                                        onDownloadClick = {
                                            if (dlState !is DownloadState.Completed) {
                                                model.bestVariantFor(data.deviceRamGb)?.let { v ->
                                                    pendingDownload = model to v
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    if (data.incompatible.isNotEmpty()) {
                        item {
                            BrowserSectionHeader(
                                title    = "✗ Too Large",
                                subtitle = "${data.incompatible.size} models need more RAM",
                                color    = Color(0xFFEF9A9A)
                            )
                        }
                        items(data.incompatible, key = { it.id }) { model ->
                            ModelCard(
                                model          = model,
                                availableRamGb = data.deviceRamGb,
                                compatible     = false,
                                downloadState  = DownloadState.Idle,
                                onDownloadClick = {}
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable private fun BrowserLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentAmber)
            Spacer(Modifier.height(12.dp))
            Text("Scanning catalog...", color = TextSubtle, fontSize = 13.sp)
        }
    }
}

@Composable private fun BrowserError() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Failed to load models.", color = Color(0xFFEF9A9A), fontSize = 14.sp)
    }
}

@Composable private fun BrowserSectionHeader(title: String, subtitle: String, color: Color) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title,    color = color,      fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextSubtle, fontSize = 11.sp)
    }
}
