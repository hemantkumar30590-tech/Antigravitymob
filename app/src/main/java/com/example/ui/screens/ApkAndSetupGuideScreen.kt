package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gateway.RepoReferenceData
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ApkAndSetupGuideScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .testTag("apk_setup_guide_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Direct APK Download Card (AI Studio & GitHub)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apk_download_instructions_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "APK Ready",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "APK File Ready to Download!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Package: com.aistudio.antigravityweb.hgkwpx (v1.0.0)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MatrixGreen,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }

                        Text(
                            text = "Yeh Android app aapke GitHub repo (Antigravityweb-onmob) ke pure Reverse Proxy, CSRF Bridge, Self-Healing Watchdog aur Fullscreen Web IDE ke saath compile ho chuki hai.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        StepBox(
                            stepNumber = "1",
                            title = "AI Studio se Direct APK Download karein (Recommended)",
                            description = "Screen ke top-right corner me Settings (⚙️) ya Export / Download menu par click karein -> 'Download APK' select karein. Aapki compiled APK turant download ho jayegi!"
                        )

                        StepBox(
                            stepNumber = "2",
                            title = "GitHub Actions se Auto-APK Release Link banayein",
                            description = "Agar aap apne GitHub repo me har commit par direct APK download link chahte hain, to niche diya gaya GitHub Actions workflow copy karke .github/workflows/android.yml me save karein."
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    openExternalBrowser(
                                        context,
                                        "http://localhost:4400/AntigravityWeb.apk"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("direct_apk_download_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MatrixGreen,
                                    contentColor = Color(0xFF00210B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Direct APK Download",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download .APK", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    openExternalBrowser(context, RepoReferenceData.REPO_URL)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_github_repo_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open GitHub Repo",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub Repo")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                copyToClipboard(
                                    context,
                                    "GitHub Actions Workflow",
                                    RepoReferenceData.GITHUB_ACTIONS_WORKFLOW_YAML
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("copy_github_workflow_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy APK Workflow",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy GitHub Actions APK Auto-Build YAML")
                        }
                    }
                }
            }

            // 2. Termux + Antigravity Bridge Companion Commands
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Termux Commands",
                                tint = WarningAmber
                            )
                            Text(
                                text = "Termux CLI & Local Backend Bridge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        CommandCopyBlock(
                            title = "Instant Run via npx (From Repo)",
                            command = RepoReferenceData.NPX_COMMAND,
                            onCopy = {
                                copyToClipboard(context, "NPX Command", RepoReferenceData.NPX_COMMAND)
                            },
                            testTag = "copy_npx_cmd_button"
                        )

                        CommandCopyBlock(
                            title = "Manual Antigravity Hub Bridge (:1235)",
                            command = RepoReferenceData.TERMUX_BRIDGE_CMD,
                            onCopy = {
                                copyToClipboard(
                                    context,
                                    "Termux Bridge Command",
                                    RepoReferenceData.TERMUX_BRIDGE_CMD
                                )
                            },
                            testTag = "copy_bridge_cmd_button"
                        )

                        CommandCopyBlock(
                            title = "Check Gateway & Backend Status",
                            command = RepoReferenceData.STATUS_COMMAND,
                            onCopy = {
                                copyToClipboard(
                                    context,
                                    "Status Command",
                                    RepoReferenceData.STATUS_COMMAND
                                )
                            },
                            testTag = "copy_status_cmd_button"
                        )
                    }
                }
            }

            // 3. Recommended Android Battery & Doze Optimization Guide (from README.md)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Recommended Mobile Settings",
                                tint = ElectricIndigo
                            )
                            Text(
                                text = "Recommended Android Settings (24/7 Persistence)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "1. Phone Settings -> Apps -> Antigravity Web (and Termux) -> Battery -> Select 'Unrestricted' (Don't optimize).\n" +
                                "2. Recent Apps me card ko Lock 🔒 karein taaki background process clear na ho.\n" +
                                "3. App ke andar CPU Wake-Lock automatically active rehta hai.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBox(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A101D))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NeonCyan),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF001F26),
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CommandCopyBlock(
    title: String,
    command: String,
    onCopy: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $title",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = command,
            style = MaterialTheme.typography.labelMedium,
            color = MatrixGreen,
            fontFamily = JetBrainsMonoFontFamily
        )
    }
}
