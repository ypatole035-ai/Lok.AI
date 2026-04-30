package com.lokai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokai.app.data.models.HFSearchResult
import com.lokai.app.viewmodel.ModelViewModel
import kotlinx.coroutines.launch

private val BgPage  = Color(0xFF0D0D0D)
private val BgCard  = Color(0xFF181818)
private val Amber   = Color(0xFFF5A623)
private val TextMain= Color(0xFFEEEEEE)
private val TextSub = Color(0xFF777777)
private val Border  = Color(0xFF333333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HFSearchScreen(onBack: () -> Unit) {
    val vm: ModelViewModel = viewModel()
    val hfState by vm.hfSearchState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("HuggingFace Search", color = TextMain, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111111))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("e.g. mistral 7b GGUF", color = TextSub) },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Amber,
                        unfocusedBorderColor = Border,
                        focusedTextColor     = TextMain,
                        unfocusedTextColor   = TextMain,
                        cursorColor          = Amber
                    ),
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { scope.launch { vm.searchHuggingFace(query) } }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Amber)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { scope.launch { vm.searchHuggingFace(query) } }
                    )
                )
            }

            Text(
                text     = "Searches for GGUF-format models. Compatible badge reflects your device RAM.",
                color    = TextSub,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            when {
                hfState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber)
                    }
                }
                hfState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(hfState.error!!, color = Color(0xFFFF5252), fontSize = 13.sp)
                        }
                    }
                }
                hfState.results.isEmpty() && hfState.hasSearched -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("No models found for \"$query\"", color = TextSub, fontSize = 13.sp)
                        }
                    }
                }
                hfState.results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Search HuggingFace for GGUF models", color = TextSub, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Try: llama, mistral, qwen, phi, gemma", color = Color(0xFF444444), fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hfState.results, key = { it.modelId }) { result ->
                            HFModelCard(result = result)
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HFModelCard(result: HFSearchResult) {
    Card(
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        shape     = RoundedCornerShape(12.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text       = result.modelId,
                color      = TextMain,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text    = if (result.sizeGb < 0.1f) "Size unknown" else "%.1f GB".format(result.sizeGb),
                    color   = TextSub,
                    fontSize= 12.sp
                )
                if (result.downloads > 0) {
                    Text("·", color = TextSub, fontSize = 12.sp)
                    Text("%,d downloads".format(result.downloads), color = TextSub, fontSize = 12.sp)
                }
                if (result.isThinkingTrained) {
                    Text("⚡ Thinking", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (!result.isCompatible) {
                Text(
                    text     = "⚠️ May exceed your device's RAM",
                    color    = Color(0xFFFF9800),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text     = "✓ Should fit on your device",
                    color    = Color(0xFF4CAF50),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text     = "To download: search \"${result.modelId}\" in the Model Browser or visit huggingface.co",
                color    = Color(0xFF555555),
                fontSize = 11.sp
            )
        }
    }
}
