package com.example.asm_final.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.asm_final.R
import com.example.asm_final.activities.HomeActivity
import com.example.asm_final.supabase.SupabaseHttp
import com.example.asm_final.ui.TeamOsScreen
import com.example.asm_final.ui.composeHost

/** Team projects have their own destination; no local substitute for this online feature. */
class TeamOsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        return composeHost(context, scenicBackground = true) {
            if (SupabaseHttp.enabled(context)) {
                TeamOsScreen(onBack = { (activity as? HomeActivity)?.navigateTo(R.id.nav_home, false) })
            } else {
                TeamOsUnavailable()
            }
        }
    }
}

@Composable
private fun TeamOsUnavailable() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.team_tab_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.team_tab_online_required),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
