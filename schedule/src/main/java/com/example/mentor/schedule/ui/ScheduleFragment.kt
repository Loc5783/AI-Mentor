package com.example.mentor.schedule.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.fragment.app.Fragment
import com.example.mentor.data.ActiveUser
import com.example.mentor.schedule.data.ScheduleRepositoryImpl
import com.example.mentor.ui.design.MentorTheme

class ScheduleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val accountId = ActiveUser.get(requireContext()).toString()
        val repository = (requireContext().applicationContext as? com.example.mentor.schedule.domain.ScheduleRepositoryProvider)?.scheduleRepository()
            ?: ScheduleRepositoryImpl(requireContext())
        
        return ComposeView(requireContext()).apply {
            setContent {
                val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                MentorTheme(dark = dark) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        ScheduleScreen(accountId, repository, onBack = {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        })
                    }
                }
            }
        }
    }
}
