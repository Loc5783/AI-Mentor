package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.competition.*
import com.example.mentor.ui.design.*
import java.util.function.Consumer

object CompetitionUi {
    @JvmStatic fun view(context: Context, model: CompetitionViewModel, refresh: Runnable,
        learn: Runnable, profile: Consumer<String>): View = composeHost(context) {
        val state by model.state().observeAsState()
        val snapshot = state?.snapshot
        val query = state?.query ?: model.query()
        val xp = query.metric == CompetitionQuery.Metric.DAILY_XP
        val board = snapshot?.board
        val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(context)
        val heroBg = if (dark) Color(0xFF2C192E) else Color(0xFFFDE8F1)

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Hero Header matching Profile UI
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(heroBg)
                    .padding(horizontal = MentorTokens.PagePadding, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trophy),
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.competition_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.competition_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("ranking-list"),
                contentPadding = PaddingValues(MentorTokens.PagePadding),
                verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)
            ) {
                // Filter Chips Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !xp,
                                onClick = { model.select(CompetitionQuery(CompetitionQuery.Metric.STREAK, query.scope)) },
                                label = { Text(stringResource(R.string.competition_streak)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = xp,
                                onClick = { model.select(CompetitionQuery(CompetitionQuery.Metric.DAILY_XP, query.scope)) },
                                label = { Text(stringResource(R.string.competition_daily)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = query.scope == CompetitionQuery.Scope.FRIENDS,
                                onClick = { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.FRIENDS)) },
                                label = { Text(stringResource(R.string.competition_friends)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = query.scope == CompetitionQuery.Scope.GLOBAL,
                                onClick = { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.GLOBAL)) },
                                label = { Text(stringResource(R.string.competition_everyone)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Personal Summary Card
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (xp) Color(0xFFFEF08A) else Color(0xFFFFEDD5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(if (xp) R.drawable.ic_lightning else R.drawable.ic_flame),
                                    contentDescription = null,
                                    tint = if (xp) Color(0xFFCA8A04) else Color(0xFFEA580C),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.competition_you),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val score = if (xp) snapshot?.dailyXp else snapshot?.streak?.toLong()
                                Text(
                                    text = if (score == null) "—" else stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, score),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (state?.loading == true) {
                    items(3) { MentorSkeleton(stringResource(R.string.ranking_loading)) }
                } else if (snapshot?.needsLogin == true || snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE || snapshot.source == CompetitionRepository.Source.LOCAL || board == null || board.entries.isEmpty()) {
                    item {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = when {
                                        snapshot?.needsLogin == true -> stringResource(R.string.ranking_login)
                                        snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE -> stringResource(R.string.ranking_error)
                                        snapshot.source == CompetitionRepository.Source.LOCAL -> stringResource(R.string.competition_disconnected)
                                        query.scope == CompetitionQuery.Scope.FRIENDS && board?.entries?.size == 1 -> stringResource(R.string.ranking_friends_empty)
                                        else -> stringResource(R.string.ranking_empty)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                MentorButton(stringResource(if (snapshot?.needsLogin == true) R.string.login_btn else R.string.ranking_retry), { refresh.run() })
                            }
                        }
                    }
                } else {
                    items(board.entries, key = { it.learner.id }) { entry ->
                        val rankBg = when (entry.rank) {
                            1 -> Color(0xFFFEF08A) // Gold
                            2 -> Color(0xFFF1F5F9) // Silver
                            3 -> Color(0xFFFFEDD5) // Bronze
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val rankTint = when (entry.rank) {
                            1 -> Color(0xFFCA8A04)
                            2 -> Color(0xFF64748B)
                            3 -> Color(0xFFC2410C)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .testTag("ranking:${entry.learner.id}")
                                .clickable { profile.accept(entry.learner.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Rank Badge
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(rankBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${entry.rank}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = rankTint
                                    )
                                }

                                // Avatar / Initials
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.learner.nickname.trim().firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                // Nickname
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = entry.learner.nickname,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.profile_joined_date, "2026"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Value
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (xp) R.drawable.ic_lightning else R.drawable.ic_flame),
                                        contentDescription = null,
                                        tint = if (xp) Color(0xFFEAB308) else Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, entry.value),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ranking_rules),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        MentorButton(stringResource(R.string.competition_learn), { learn.run() })
                    }
                }
            }
        }
    }
}
