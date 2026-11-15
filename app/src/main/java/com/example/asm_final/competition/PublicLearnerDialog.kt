package com.example.asm_final.competition

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.asm_final.R
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.dashboard.OverviewUpdates
import com.example.asm_final.ui.PublicProfileContent
import com.example.asm_final.ui.composeHost
import com.example.mentor.data.ActiveUser
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PublicLearnerDialog : DialogFragment() {
    private lateinit var model: PublicProfileViewModel
    private var live: OverviewUpdates.Subscription? = null

    companion object {
        @JvmStatic
        fun create(id: String): PublicLearnerDialog {
            val dialog = PublicLearnerDialog()
            dialog.arguments = Bundle().apply { putString("PROFILE_ID", id) }
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        model = ViewModelProvider(this)[PublicProfileViewModel::class.java]
        
        val view = composeHost(requireContext()) {
            val state by model.state().observeAsState(PublicProfileViewModel.State(null, null, true, false, false))
            PublicProfileContent(
                learner = state.learner,
                avatar = state.avatar,
                loading = state.loading,
                failed = state.failed,
                onRetry = { model.refresh() }
            )
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ranking_profile)
            .setView(view)
            .setPositiveButton(R.string.ranking_close, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
        val app = requireActivity().application as? AiMentorApplication
        live = app?.services()?.publicProfileUpdates(requireContext(), model.profileId)
            ?.observe(ActiveUser.get(requireContext())) { model.refresh() }
    }

    override fun onPause() {
        live?.close()
        live = null
        super.onPause()
    }
}
