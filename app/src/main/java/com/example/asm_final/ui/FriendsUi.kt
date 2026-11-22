package com.example.asm_final.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.asm_final.R
import com.example.asm_final.competition.CompetitionViewModel
import com.example.mentor.ui.design.*
@Composable
internal fun FriendsPanel(
    state: CompetitionViewModel.FriendsState?,
    search: CompetitionViewModel.SearchState?,
    palette: PremiumDashboardPalette,
    model: CompetitionViewModel,
    openProfile: (String) -> Unit,
    searchMode: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    PremiumDashboardCard(palette) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.friends_manage_title), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = palette.title)
                Text(stringResource(R.string.friends_manage_subtitle), style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary)
            }
            TextButton(onClick = { model.refreshFriends() }) {
                Text(stringResource(R.string.ranking_retry), color = palette.accent)
            }
        }
        if (searchMode) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it.take(40) },
                label = { Text(stringResource(R.string.friends_search_hint)) },
                singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { model.searchLearners(query) },
                enabled = query.trim().length >= 2 && search?.loading != true) {
                Text(stringResource(R.string.friends_search_action))
            }
        }
        when {
            search?.loading == true -> MentorSkeleton(stringResource(R.string.ranking_loading))
            search?.failed == true -> MentorErrorNotice(stringResource(R.string.friends_search_error))
            search?.searched == true -> {
                val found = search.items.filterNot { model.isCurrentLearner(it.id) }
                if (found.isEmpty()) Text(stringResource(R.string.friends_search_empty),
                    color = palette.secondary, style = MaterialTheme.typography.bodySmall)
                found.forEach { learner ->
                    val existing = state?.items?.firstOrNull { it.profileId == learner.id }
                    Surface(Modifier.fillMaxWidth().clickable { openProfile(learner.id) },
                        shape = RoundedCornerShape(14.dp), color = palette.elevated,
                        border = BorderStroke(1.dp, palette.border)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(palette.iconSurface),
                                contentAlignment = Alignment.Center) {
                                Text(learner.nickname.trim().firstOrNull()?.uppercase() ?: "•",
                                    color = palette.title, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(learner.nickname, style = MaterialTheme.typography.titleSmall,
                                    color = palette.title, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.friends_view_profile),
                                    style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                            }
                            if (existing == null) TextButton(onClick = {
                                model.sendFriendRequest(learner.id)
                            }, enabled = state != null && !state.loading) {
                                Text(stringResource(R.string.friends_add), color = palette.accent)
                            } else Text(stringResource(if (existing.state.equals("ACCEPTED", true))
                                R.string.friends_already_connected else if (existing.state.equals("PENDING", true)
                                    && existing.direction.equals("INCOMING", true))
                                R.string.friends_request_incoming_short else if (existing.state.equals("PENDING", true))
                                R.string.friends_request_pending else R.string.friends_request_unavailable),
                                style = MaterialTheme.typography.labelSmall, color = palette.secondary)
                        }
                    }
                }
            }
        }
        } else {
        HorizontalDivider(color = palette.border)
        when {
            state == null || state.loading && state.items.isEmpty() -> MentorSkeleton(stringResource(R.string.ranking_loading))
            state.failed && state.items.isEmpty() -> MentorErrorNotice(stringResource(when (state.message) {
                "LOAD_OFFLINE", "ACTION_OFFLINE" -> R.string.friends_offline_error
                "LOAD_LOGIN_REQUIRED", "ACTION_LOGIN_REQUIRED" -> R.string.friends_login_error
                "ACTION_FAILED" -> R.string.friends_action_error
                else -> R.string.friends_load_error
            }))
            else -> {
                val visible = state.items.filter { it.state.equals("PENDING", true) || it.state.equals("ACCEPTED", true) }
                if (visible.isEmpty()) Text(stringResource(R.string.friends_empty),
                    color = palette.secondary, style = MaterialTheme.typography.bodySmall)
                visible.forEach { friend ->
                    HorizontalDivider(color = palette.border)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).clickable { openProfile(friend.profileId) },
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(friend.nickname.ifBlank { stringResource(R.string.friends_learner) },
                                style = MaterialTheme.typography.titleSmall, color = palette.title,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    friend.state.equals("ACCEPTED", true) -> stringResource(R.string.friends_personal_streak, friend.streakDays)
                                    friend.direction.equals("INCOMING", true) -> stringResource(R.string.friends_incoming)
                                    else -> stringResource(R.string.friends_outgoing)
                                },
                                style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                            if (friend.state.equals("ACCEPTED", true)) {
                                Text(
                                    stringResource(R.string.friends_shared_streak, friend.sharedStreakDays),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.accent
                                )
                            }
                        }
                        when {
                            friend.state.equals("PENDING", true) && friend.direction.equals("INCOMING", true) -> {
                                TextButton(onClick = { model.decideFriendship(friend, "ACCEPT") }, enabled = !state.loading) {
                                    Text(stringResource(R.string.friends_accept), color = palette.accent)
                                }
                                TextButton(onClick = { model.decideFriendship(friend, "DECLINE") }, enabled = !state.loading) {
                                    Text(stringResource(R.string.friends_decline), color = palette.secondary)
                                }
                            }
                            friend.state.equals("ACCEPTED", true) -> TextButton(onClick = { model.removeFriend(friend) }, enabled = !state.loading) {
                                Text(stringResource(R.string.friends_remove), color = palette.secondary)
                            }
                        }
                    }
                }
            }
        }
        }
        if (!state?.message.isNullOrBlank()) {
            val res = when (state?.message) {
                "REQUEST_SENT" -> R.string.friends_request_sent
                "FRIEND_ACCEPTED" -> R.string.friends_accepted
                "REQUEST_DECLINED" -> R.string.friends_declined
                "FRIEND_REMOVED" -> R.string.friends_removed
                else -> null
            }
            if (res != null) Text(stringResource(res), color = palette.accent,
                style = MaterialTheme.typography.bodySmall)
        }
        if (state?.failed == true && state.items.isNotEmpty())
            MentorErrorNotice(stringResource(when (state.message) {
                "LOAD_OFFLINE", "ACTION_OFFLINE" -> R.string.friends_offline_error
                "LOAD_LOGIN_REQUIRED", "ACTION_LOGIN_REQUIRED" -> R.string.friends_login_error
                "LOAD_FAILED" -> R.string.friends_load_error
                else -> R.string.friends_action_error
            }))
    }
}
