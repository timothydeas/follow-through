package com.example.grounded.ui.followthrough

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grounded.ui.rememberA11yAnnouncer
import com.example.grounded.ui.theme.DmSansFontFamily
import com.example.grounded.ui.theme.PoppinsFontFamily
import com.example.grounded.ui.theme.PrimaryForge
import kotlinx.coroutines.delay

private val ReassuranceBg = Color(0xFFA8431E)

@Composable
fun FollowThroughEntryScreen(
    viewModel: FollowThroughViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val announce = rememberA11yAnnouncer()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main entry UI
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = viewModel::save,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PrimaryForge
                        )
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = DmSansFontFamily
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Linked note title in muted text
                if (uiState.noteTitle.isNotEmpty()) {
                    Text(
                        text = uiState.noteTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = DmSansFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Body text field
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.body.isEmpty()) {
                        Text(
                            text = "What did you do?",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = DmSansFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    BasicTextField(
                        value = uiState.body,
                        onValueChange = viewModel::onBodyChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(PrimaryForge),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Reassurance overlay — shown after saving
        if (uiState.isSaved) {
            LaunchedEffect(Unit) {
                announce("You followed through. That's what matters. Regardless of the outcome.")
                keyboardController?.hide()
                delay(2000)
                onNavigateToList()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReassuranceBg)
                    .clickable(onClickLabel = "Dismiss", onClick = onNavigateToList)
                    .semantics {
                        contentDescription =
                            "You followed through. That's what matters. Regardless of the outcome."
                        liveRegion = LiveRegionMode.Assertive
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You followed through, in spite of nerves or outcomes.",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        }
    }
}
