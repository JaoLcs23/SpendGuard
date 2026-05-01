package com.joaolucas.spendguard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievementsManager: AchievementsManager,
    onBack: () -> Unit
) {
    val gold          = MaterialTheme.colorScheme.primary
    val achievements  = achievementsManager.all
    val unlockedCount = achievementsManager.unlockedCount()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = gold.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$unlockedCount de ${achievements.size} conquistas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = gold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress   = unlockedCount.toFloat() / achievements.size,
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color      = gold,
                            trackColor = gold.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${achievements.size - unlockedCount} restantes",
                            style = MaterialTheme.typography.labelSmall,
                            color = gold.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding        = PaddingValues(bottom = 24.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        unlocked    = achievementsManager.isUnlocked(achievement.id)
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, unlocked: Boolean) {
    val gold = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = when {
                unlocked          -> MaterialTheme.colorScheme.secondaryContainer
                achievement.isPro -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else              -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (achievement.isPro && !unlocked) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape    = RoundedCornerShape(20.dp),
                        color    = gold.copy(alpha = 0.15f),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            "PRO",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style       = MaterialTheme.typography.labelSmall,
                            color       = gold,
                            fontWeight  = FontWeight.ExtraBold,
                            fontSize    = 8.sp
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape    = RoundedCornerShape(24.dp),
                color    = if (unlocked) gold.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (unlocked) achievement.icon else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = when {
                            unlocked          -> gold
                            achievement.isPro -> gold.copy(alpha = 0.3f)
                            else              -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                achievement.title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = when {
                    unlocked          -> gold
                    achievement.isPro -> gold.copy(alpha = 0.4f)
                    else              -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.Center
            )

            Text(
                if (unlocked) achievement.description else achievement.hint,
                style     = MaterialTheme.typography.bodySmall,
                color     = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            if (unlocked) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = gold.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(achievement.icon, null, tint = gold, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Conquistado",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}