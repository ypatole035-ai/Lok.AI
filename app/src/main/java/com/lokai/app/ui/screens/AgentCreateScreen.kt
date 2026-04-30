package com.lokai.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.data.agent.FileProcessor
import com.lokai.app.model.AgentCategory
import com.lokai.app.model.DownloadedModel
import com.lokai.app.ui.components.CategoryBadge
import com.lokai.app.viewmodel.AgentViewModel

private val BgPage  = Color(0xFF0D0D0D)
private val BgCard  = Color(0xFF181818)
private val Amber   = Color(0xFFF5A623)
private val TextMain= Color(0xFFEEEEEE)
private val TextSub = Color(0xFF777777)

@Composable
fun AgentCreateScreen(
    downloadedModels: List<DownloadedModel>,
    onBack:           () -> Unit,
    onCreated:        () -> Unit
) {
    val vm:    AgentViewModel = viewModel()
    val state by vm.createState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    // Navigate away on success
    LaunchedEffect(state.done) {
        if (state.done) {
            vm.resetCreateState()
            onCreated()
        }
    }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Persist read permission
        ctx.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val path = uri.path ?: return@rememberLauncherForActivityResult
        // Get display name
        val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIdx >= 0) cursor.getString(nameIdx) else uri.lastPathSegment
        } ?: uri.lastPathSegment ?: "file"

        // Use the real file path via content resolver (copy to cache)
        val inputStream = ctx.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val ext = name.substringAfterLast('.', "")
            val cacheFile = java.io.File(ctx.cacheDir, "agent_file_${System.currentTimeMillis()}.$ext")
            cacheFile.outputStream().use { out -> inputStream.copyTo(out) }
            vm.onFileSelected(cacheFile.absolutePath, name)
        }
    }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title  = { Text("New Agent", color = TextMain, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPage)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Agent Name ────────────────────────────────────────────────────
            SectionLabel("Agent Name")
            OutlinedTextField(
                value         = state.name,
                onValueChange = vm::onNameChange,
                placeholder   = { Text("e.g. Backend Code, Chapter 3 Helper…", color = TextSub) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = agentTextFieldColors()
            )

            // ── Category ──────────────────────────────────────────────────────
            SectionLabel("Category")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AgentCategory.entries) { cat ->
                    CategoryBadge(
                        emoji    = cat.emoji,
                        label    = cat.displayName,
                        selected = state.category == cat,
                        onClick  = { vm.onCategoryChange(cat) }
                    )
                }
            }
            // Category description
            Text(
                text     = state.category.description,
                color    = TextSub,
                fontSize = 12.sp
            )

            // ── Model ─────────────────────────────────────────────────────────
            SectionLabel("Model")
            if (downloadedModels.isEmpty()) {
                Text("No downloaded models. Download a model first.", color = TextSub, fontSize = 13.sp)
            } else {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value         = state.modelName.ifBlank { "Select model…" },
                        onValueChange = {},
                        readOnly      = true,
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        colors        = agentTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false },
                        modifier         = Modifier.exposedDropdownSize()
                    ) {
                        downloadedModels.forEach { model ->
                            DropdownMenuItem(
                                text    = { Text(model.name, color = TextMain) },
                                onClick = {
                                    vm.onModelChange(model.id, model.name)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── File (optional) ───────────────────────────────────────────────
            SectionLabel("Attach File (optional)")
            if (state.fileName != null) {
                Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📄", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(state.fileName!!, color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.onFileSelected("", "") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove file", tint = TextSub)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val mimeTypes = arrayOf("text/*", "application/pdf",
                            "application/json", "application/xml")
                        filePicker.launch(mimeTypes)
                    },
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    shape  = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, tint = Amber)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose file…", color = TextMain)
                }
                Text(
                    text  = "Supported: .txt, .md, .pdf, code files (.kt, .py, .js, .ts, .java…), .json, .xml, .yaml",
                    color = TextSub, fontSize = 11.sp
                )
            }

            // ── System Prompt ─────────────────────────────────────────────────
            SectionLabel("System Prompt")
            OutlinedTextField(
                value         = state.systemPrompt,
                onValueChange = vm::onSystemPromptChange,
                minLines      = 3,
                maxLines      = 6,
                modifier      = Modifier.fillMaxWidth(),
                colors        = agentTextFieldColors(),
                placeholder   = { Text("Auto-generated from category. Edit freely.", color = TextSub) }
            )

            // ── Custom settings (only shown for CUSTOM) ───────────────────────
            if (state.category == AgentCategory.CUSTOM) {
                CustomSettingsSection(state, vm)
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (state.error != null && !state.saving) {
                Text(state.error!!, color = Color(0xFFFF5252), fontSize = 13.sp)
            }
            if (state.saving) {
                val msg = state.error?.removePrefix("Indexing: ") ?: "Saving…"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = TextSub, fontSize = 13.sp)
                }
            }

            // ── Create button ─────────────────────────────────────────────────
            Button(
                onClick  = vm::saveAgent,
                enabled  = !state.saving && state.name.isNotBlank() && state.modelId.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)
            ) {
                Text("Create Agent", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CustomSettingsSection(state: com.lokai.app.viewmodel.AgentCreateUiState, vm: AgentViewModel) {
    val strategies = listOf("Full Load", "Skeleton+Retrieval", "Summary+Retrieval", "Pure Retrieval")

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Custom Strategy Settings", color = Color(0xFFF5A623), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            // Strategy picker
            var stratExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = stratExpanded, onExpandedChange = { stratExpanded = it }) {
                OutlinedTextField(
                    value         = state.customStrategy,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Reading Strategy", color = TextSub) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(stratExpanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    colors        = agentTextFieldColors()
                )
                ExposedDropdownMenu(expanded = stratExpanded, onDismissRequest = { stratExpanded = false }) {
                    strategies.forEach { s ->
                        DropdownMenuItem(text = { Text(s, color = TextMain) }, onClick = {
                            vm.onCustomStrategyChange(s); stratExpanded = false
                        })
                    }
                }
            }

            // Chunk size
            Text("Chunk Size: ${state.customChunkSize} words", color = TextMain, fontSize = 13.sp)
            Slider(value = state.customChunkSize.toFloat(), onValueChange = { vm.onCustomChunkSizeChange(it.toInt()) },
                   valueRange = 100f..500f, steps = 7, colors = sliderColors())

            // Chunks retrieved
            Text("Chunks Per Message: ${state.customChunksRetrieved}", color = TextMain, fontSize = 13.sp)
            Slider(value = state.customChunksRetrieved.toFloat(), onValueChange = { vm.onCustomChunksRetrievedChange(it.toInt()) },
                   valueRange = 1f..10f, steps = 8, colors = sliderColors())

            // Temperature
            Text("Temperature: ${"%.1f".format(state.customTemperature)}", color = TextMain, fontSize = 13.sp)
            Slider(value = state.customTemperature, onValueChange = vm::onCustomTemperatureChange,
                   valueRange = 0f..2f, steps = 19, colors = sliderColors())

            // Max tokens
            Text("Max Tokens: ${state.customMaxTokens}", color = TextMain, fontSize = 13.sp)
            Slider(value = state.customMaxTokens.toFloat(), onValueChange = { vm.onCustomMaxTokensChange(it.toInt()) },
                   valueRange = 50f..2048f, steps = 19, colors = sliderColors())

            // Fallback toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fallback Retrieval", color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = state.customFallback, onCheckedChange = vm::onCustomFallbackChange,
                       colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFF5A623)))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFFF5A623), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
         letterSpacing = 0.5.sp)
}

@Composable
private fun agentTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFFF5A623),
    unfocusedBorderColor = Color(0xFF333333),
    focusedTextColor     = Color(0xFFEEEEEE),
    unfocusedTextColor   = Color(0xFFEEEEEE),
    cursorColor          = Color(0xFFF5A623)
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor        = Color(0xFFF5A623),
    activeTrackColor  = Color(0xFFF5A623),
    inactiveTrackColor= Color(0xFF333333)
)
