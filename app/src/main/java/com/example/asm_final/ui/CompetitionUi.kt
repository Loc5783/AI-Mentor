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
import androidx.compose.ui.semantics.Role
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
        val palette = premiumDashboardPalette(dark)

        Column(Modifier.fillMaxSize()) {
            PremiumDashboardHeader(palette, artwork = com.example.mentor.ui.design.MentorArtwork.GOALS) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(palette.iconSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trophy),
                            contentDescription = null,
                            tint = palette.experience,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.competition_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.title
                        )
                        Text(
                            text = stringResource(R.string.competition_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondary
                        )
                    }
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("ranking-list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumDashboardSegment(
                                label = stringResource(R.string.competition_streak),
                                selected = !xp,
                                palette = palette,
                                onClick = { model.select(CompetitionQuery(CompetitionQuery.Metric.STREAK, query.scope)) },
                                modifier = Modifier.weight(1f)
                            )
                            PremiumDashboardSegment(
                                label = stringResource(R.string.competition_daily),
                                selected = xp,
                                palette = palette,
                                onClick = { model.select(CompetitionQuery(CompetitionQuery.Metric.DAILY_XP, query.scope)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumDashboardSegment(
                                label = stringResource(R.string.competition_friends),
                                selected = query.scope == CompetitionQuery.Scope.FRIENDS,
                                palette = palette,
                                onClick = { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.FRIENDS)) },
                                modifier = Modifier.weight(1f)
                            )
                            PremiumDashboardSegment(
                                label = stringResource(R.string.competition_everyone),
                                selected = query.scope == CompetitionQuery.Scope.GLOBAL,
                                palette = palette,
                                onClick = { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.GLOBAL)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    PremiumDashboardCard(palette, highlighted = true) {
                        Text(stringResource(R.string.competition_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = palette.title)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(palette.iconSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(if (xp) R.drawable.ic_lightning else R.drawable.ic_flame),
                                    contentDescription = null,
                                    tint = if (xp) palette.experience else palette.streak,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(if (xp) R.string.competition_daily else R.string.competition_streak),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondary
                                )
                                val score = if (xp) snapshot?.dailyXp else snapshot?.streak?.toLong()
                                Text(
                                    text = if (score == null) "—" else stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, score),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.title
                                )
                            }
                        }
                    }
                }

                if (state?.loading == true) {
                    items(3) { MentorSkeleton(stringResource(R.string.ranking_loading)) }
                } else if (snapshot?.needsLogin == true || snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE || snapshot.source == CompetitionRepository.Source.LOCAL || board == null || board.entries.isEmpty()) {
                    item {
                        PremiumDashboardCard(palette) {
                            MentorIllustration(MentorArtwork.GOALS)
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(palette.iconSurface).align(Alignment.CenterHorizontally),
                                    contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ic_trophy), null,
                                        Modifier.size(26.dp), tint = palette.accent)
                                }
                                val unavailable = snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE
                                if (unavailable && snapshot?.needsLogin != true) {
                                    Text(stringResource(R.string.competition_error_title),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                        color = palette.title, modifier = Modifier.align(Alignment.CenterHorizontally))
                                }
                                Text(
                                    text = when {
                                        snapshot?.needsLogin == true -> stringResource(R.string.ranking_login)
                                        unavailable -> stringResource(R.string.competition_error_subtitle)
                                        snapshot.source == CompetitionRepository.Source.LOCAL -> stringResource(R.string.competition_disconnected)
                                        query.scope == CompetitionQuery.Scope.FRIENDS && board?.entries?.size == 1 -> stringResource(R.string.ranking_friends_empty)
                                        else -> stringResource(R.string.ranking_empty)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                PremiumDashboardAction(stringResource(if (snapshot?.needsLogin == true) R.string.login_btn else R.string.ranking_retry),
                                    palette, { refresh.run() }, Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                } else {
                    items(board.entries, key = { it.learner.id }) { entry ->
                        val rankTint = when (entry.rank) {
                            1 -> palette.experience
                            2 -> palette.secondary
                            3 -> palette.streak
                            else -> palette.muted
                        }

                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .testTag("ranking:${entry.learner.id}")
                                .clickable(onClickLabel = stringResource(R.string.ranking_profile),
                                    role = Role.Button) { profile.accept(entry.learner.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Rank Badge
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(palette.iconSurface),
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
                                        .background(palette.elevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.learner.nickname.trim().firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.title
                                    )
                                }

                                // Nickname
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = entry.learner.nickname,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(if (xp) R.string.competition_daily else R.string.competition_streak),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondary
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
                                        tint = if (xp) palette.experience else palette.streak,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, entry.value),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.title
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
                            color = palette.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        PremiumDashboardAction(stringResource(R.string.competition_learn), palette, { learn.run() })
                    }
                }
            }
        }
    }
}
