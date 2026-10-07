package com.freshsave.app.feature.business.listing

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.DealImage
import com.freshsave.app.ui.components.FilterChip
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.FsSlider
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.NavIcon
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatTaka
import com.freshsave.app.util.formatTimeRange
import kotlin.math.roundToInt

@Composable
fun CreateListingRoute(editDealId: String?, onClose: () -> Unit, onPublished: () -> Unit) {
    val viewModel = appViewModel(key = "listing-${editDealId ?: "new"}") {
        CreateListingViewModel(editDealId, it.clock, it.dealRepository, it.businessRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current

    // Opens the system photo picker (no storage permission needed).
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            // Keep read access after an app restart; harmless if the provider doesn't allow it.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.update { it.copy(imageUrl = uri.toString()) }
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onPublished()
            snackbar.showSnackbar(if (state.editing) "Listing updated" else "Your listing is live!")
        }
    }

    // Reading LocalNow keeps the "Pickup 6:30–8:30 pm" preview in step with the clock.
    val (start, end) = viewModel.pickupWindow(state.form, LocalNow.current)
    CreateListingScreen(
        state = state,
        windowLabel = formatTimeRange(start, end),
        onClose = onClose,
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onChange = viewModel::update,
        onPublish = viewModel::publish,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateListingScreen(
    state: CreateListingUiState,
    windowLabel: String,
    onClose: () -> Unit,
    onPickPhoto: () -> Unit,
    onChange: ((ListingForm) -> ListingForm) -> Unit,
    onPublish: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val form = state.form
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        FsTopBar(if (state.editing) "Edit listing" else "New listing", navIcon = NavIcon.Close, onNavClick = onClose)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            PhotoBox(form.imageUrl, onPickPhoto)
            Spacer(Modifier.height(18.dp))
            FsTextField(
                value = form.title,
                onValueChange = { v -> onChange { it.copy(title = v) } },
                label = "Item name",
                isError = "title" in state.errors,
            )
            ErrorText(state.errors["title"])
            Spacer(Modifier.height(16.dp))
            Text("Category", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodCategory.entries.forEach { c ->
                    FilterChip(c.label, form.category == c, { onChange { it.copy(category = c) } })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FsTextField(
                        value = form.originalPrice,
                        onValueChange = { v -> onChange { it.copy(originalPrice = v.filter(Char::isDigit).take(5)) } },
                        label = "Original ৳",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = "price" in state.errors,
                    )
                    ErrorText(state.errors["price"])
                }
                Column(Modifier.weight(1f)) {
                    FsTextField(
                        value = form.quantity,
                        onValueChange = { v -> onChange { it.copy(quantity = v.filter(Char::isDigit).take(3)) } },
                        label = "Quantity",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = "quantity" in state.errors,
                    )
                    ErrorText(state.errors["quantity"])
                }
            }
            Spacer(Modifier.height(16.dp))
            FsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Discount", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("${form.discountPercent}% off", style = MaterialTheme.typography.titleMedium, color = colors.orange)
                }
                FsSlider(
                    value = form.discountPercent.toFloat(),
                    onValueChange = { v -> onChange { it.copy(discountPercent = (v / 5).roundToInt() * 5) } },
                    valueRange = 10f..90f,
                    activeColor = colors.orange,
                )
                // The sale price is derived from the form, so it updates as the slider moves.
                Text(
                    buildAnnotatedString {
                        append("Sells for ")
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)) { append(formatTaka(form.salePrice)) }
                        append(" · you keep ${formatTaka(form.salePrice)}/bag")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.green,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Pickup window (auto-expires)", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Starts", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StartOption.entries.forEach { o -> FilterChip(o.label, form.start == o, { onChange { it.copy(start = o) } }) }
            }
            Spacer(Modifier.height(10.dp))
            Text("Open for", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WindowOption.entries.forEach { o -> FilterChip(o.label, form.window == o, { onChange { it.copy(window = o) } }) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Pickup $windowLabel today", style = MaterialTheme.typography.titleSmall, color = colors.green)
            Spacer(Modifier.height(16.dp))
            FsTextField(
                value = form.description,
                onValueChange = { v -> onChange { it.copy(description = v.take(300)) } },
                label = "Description (optional)",
                placeholder = "What might be inside? Any allergens?",
                singleLine = false,
                minLines = 2,
            )
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            text = if (state.editing) "Save changes" else "Publish listing",
            onClick = onPublish,
            loading = state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun PhotoBox(imageUrl: String?, onClick: () -> Unit) {
    val colors = FreshSaveTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            DealImage(imageUrl, "Listing photo", Modifier.fillMaxSize(), shape)
        } else {
            DealImage(null, null, Modifier.fillMaxSize(), shape)
            // Dashed outline like the "Add photo" box in the mock-up.
            val outline = colors.outline
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = outline.copy(alpha = 1f),
                    style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddAPhoto, null, tint = colors.textMuted, modifier = Modifier.size(28.dp))
                Text("Add photo", style = MaterialTheme.typography.titleSmall, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun ErrorText(message: String?) {
    if (message != null) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = FreshSaveTheme.colors.danger, modifier = Modifier.padding(top = 4.dp))
    }
}

@Preview(widthDp = 380, heightDp = 1000)
@Composable
private fun CreateListingPreview() {
    FreshSaveTheme {
        CreateListingScreen(CreateListingUiState(), "6:30–8:30 pm", {}, {}, {}, {})
    }
}
