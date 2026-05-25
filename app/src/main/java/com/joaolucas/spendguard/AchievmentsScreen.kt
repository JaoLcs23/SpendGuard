package com.joaolucas.spendguard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievementsManager: AchievementsManager,
    onBack: () -> Unit
) {
    val gold          = MaterialTheme.colorScheme.primary
    val achievements  = achievementsManager.all
    val unlockedCount = achievementsManager.unlockedCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = gold)
                }
                Text(
                    "Conquistas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = gold.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Seu Progresso",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$unlockedCount de ${achievements.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = gold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress   = unlockedCount.toFloat() / achievements.size,
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color      = gold,
                            trackColor = gold.copy(alpha = 0.2f),
                            strokeCap  = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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


@Composable
fun AchievementCard(achievement: Achievement, unlocked: Boolean) {
    val gold = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(
            containerColor = when {
                unlocked          -> MaterialTheme.colorScheme.secondaryContainer
                achievement.isPro -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                else              -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 2.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (achievement.isPro && !unlocked) {
                Surface(
                    shape    = RoundedCornerShape(bottomStart = 10.dp, topEnd = 18.dp),
                    color    = gold.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        "PRO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style       = MaterialTheme.typography.labelSmall,
                        color       = gold,
                        fontWeight  = FontWeight.ExtraBold,
                        fontSize    = 9.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(if (achievement.isPro && !unlocked) 8.dp else 0.dp))
                
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape    = RoundedCornerShape(26.dp),
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    achievement.title,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = when {
                        unlocked          -> gold
                        achievement.isPro -> gold.copy(alpha = 0.4f)
                        else              -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    if (unlocked) achievement.description else achievement.hint,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                if (unlocked) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = gold.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(achievement.icon, null, tint = gold, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Desbloqueada",
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
}