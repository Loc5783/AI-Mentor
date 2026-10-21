package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
        LazyColumn(Modifier.fillMaxSize().testTag("ranking-list").background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
            item {
                Text(stringResource(R.string.competition_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.competition_description), Modifier.padding(top = MentorTokens.SpaceSmall))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
                    FilterChip(!xp, { model.select(CompetitionQuery(CompetitionQuery.Metric.STREAK, query.scope)) },
                        label = { Text(stringResource(R.string.competition_streak)) })
                    FilterChip(xp, { model.select(CompetitionQuery(CompetitionQuery.Metric.DAILY_XP, query.scope)) },
                        label = { Text(stringResource(R.string.competition_daily)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
                    FilterChip(query.scope == CompetitionQuery.Scope.FRIENDS,
                        { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.FRIENDS)) },
                        label = { Text(stringResource(R.string.competition_friends)) })
                    FilterChip(query.scope == CompetitionQuery.Scope.GLOBAL,
                        { model.select(CompetitionQuery(query.metric, CompetitionQuery.Scope.GLOBAL)) },
                        label = { Text(stringResource(R.string.competition_everyone)) })
                }
            }
            item {
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.competition_you), style = MaterialTheme.typography.labelLarge)
                    val score = if (xp) snapshot?.dailyXp else snapshot?.streak?.toLong()
                    Text(if (score == null) "—" else stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, score),
                        style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(when {
                        snapshot == null -> R.string.ranking_loading
                        snapshot.source == CompetitionRepository.Source.LOCAL -> R.string.competition_local
                        snapshot.cached -> R.string.overview_cached
                        snapshot.dailyXp == null -> R.string.overview_error
                        else -> R.string.overview_current
                    }), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state?.loading == true) items(3) { MentorSkeleton(stringResource(R.string.ranking_loading)) }
            else item {
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(when {
                        snapshot?.needsLogin == true -> stringResource(R.string.ranking_login)
                        snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE -> stringResource(R.string.ranking_error)
                        snapshot.source == CompetitionRepository.Source.LOCAL -> stringResource(R.string.competition_disconnected)
                        board == null || board.entries.isEmpty() -> stringResource(R.string.ranking_empty)
                        query.scope == CompetitionQuery.Scope.FRIENDS && board.entries.size == 1 -> stringResource(R.string.ranking_friends_empty)
                        else -> stringResource(R.string.ranking_day, board.learningDay)
                    })
                    MentorButton(stringResource(if (snapshot?.needsLogin == true) R.string.login_btn else R.string.ranking_retry), { refresh.run() })
                }
            }
            items(board?.entries.orEmpty(), key = { it.learner.id }) { entry ->
                MentorCard(Modifier.fillMaxWidth().testTag("ranking:${entry.learner.id}").clickable { profile.accept(entry.learner.id) }) {
                    Text("${entry.rank} · ${entry.learner.nickname}", style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(if (xp) R.string.competition_xp_value else R.string.competition_streak_value, entry.value))
                }
            }
            item {
                Text(stringResource(R.string.ranking_rules), style = MaterialTheme.typography.bodySmall)
                MentorButton(stringResource(R.string.competition_learn), { learn.run() })
            }
        }
    }
}
