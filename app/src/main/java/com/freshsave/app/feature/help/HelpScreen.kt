package com.freshsave.app.feature.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.RowDivider
import com.freshsave.app.ui.components.RowGroup
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.launch

data class Faq(val question: String, val answer: String)

// Static help content; a CMS or Firebase Remote Config could serve this later.
val faqs = listOf(
    Faq(
        "How do I pick up my order?",
        "Show the 4-character code (or QR) from your order to staff within the pickup window. That's it — no printing needed.",
    ),
    Faq(
        "Can I get a refund?",
        "If a shop cancels or runs out, you're refunded automatically to your bKash, Nagad or card within 3–5 days. Missed pickups can't be refunded.",
    ),
    Faq(
        "What's in a surprise bag?",
        "Whatever the shop has left that day — usually worth 2–3× what you pay. Check the listing for the category and ask staff about allergens.",
    ),
    Faq("Which payments are accepted?", "bKash, Nagad, and Visa or Mastercard debit/credit cards."),
    Faq(
        "Is the food safe to eat?",
        "Yes. Shops only list food that's still good to eat today; it simply didn't sell. Eat it the same day unless the shop says otherwise.",
    ),
    Faq(
        "How do I list surplus as a business?",
        "Switch to a business account from Profile, then tap \"Post surplus listing\" on your dashboard.",
    ),
)

@Composable
fun HelpRoute(onBack: () -> Unit) {
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    HelpScreen(
        onBack = onBack,
        // TODO: open an in-app chat (e.g. Intercom / Firebase) once support is staffed.
        onChat = { scope.launch { snackbar.showSnackbar("Live chat is coming soon. Email help@freshsave.app meanwhile.") } },
    )
}

@Composable
fun HelpScreen(onBack: () -> Unit, onChat: () -> Unit) {
    val colors = FreshSaveTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    // Index of the open question; the first one starts open, like the mock-up.
    var expanded by rememberSaveable { mutableStateOf<Int?>(0) }
    val visible = faqs.withIndex().filter { (_, faq) ->
        query.isBlank() || faq.question.contains(query, true) || faq.answer.contains(query, true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        FsTopBar("Help & support", onNavClick = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            FsTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search help articles",
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textMuted) },
            )
            Spacer(Modifier.height(20.dp))
            Text("Popular questions", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            if (visible.isEmpty()) {
                EmptyState("No articles found", "Try another word, or chat with us below.", icon = Icons.Outlined.SearchOff)
            } else {
                RowGroup {
                    visible.forEachIndexed { position, (index, faq) ->
                        if (position > 0) RowDivider()
                        FaqItem(
                            faq = faq,
                            open = expanded == index || query.isNotBlank(),
                            onToggle = { expanded = if (expanded == index) null else index },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            ChatBanner(onChat)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqItem(faq: Faq, open: Boolean, onToggle: () -> Unit) {
    val colors = FreshSaveTheme.colors
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(Modifier.clickable(onClick = onToggle)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(faq.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                if (open) "Collapse" else "Expand",
                tint = colors.green,
                modifier = Modifier.rotate(rotation),
            )
        }
        // AnimatedVisibility slides the answer open and closed.
        AnimatedVisibility(open) {
            Text(
                faq.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .background(colors.surfaceMuted, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun ChatBanner(onChat: () -> Unit) {
    val colors = FreshSaveTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(colors.green, Color(0xFF2E9A62))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Still need help?", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Chat with us · avg reply 4 min", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
        }
        Text(
            "Chat",
            style = MaterialTheme.typography.labelLarge,
            color = colors.green,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(onClick = onChat)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun HelpPreview() {
    FreshSaveTheme { HelpScreen({}, {}) }
}
