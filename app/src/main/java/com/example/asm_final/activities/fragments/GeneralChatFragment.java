package com.example.asm_final.activities.fragments;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.example.asm_final.R;
import com.example.asm_final.chat.*;
import com.example.mentor.data.ActiveUser;
import com.google.android.material.chip.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;
import java.util.*;

public final class GeneralChatFragment extends Fragment {
  private ChatViewModel model;
  private View root;
  private boolean initialChatApplied;
  private String cameraName, cameraThread;
  private ArrayList<Uri> pendingUris = new ArrayList<>();
  private boolean pendingCamera;
  private final ActivityResultLauncher<String[]> documents =
      registerForActivityResult(
          new ActivityResultContracts.OpenMultipleDocuments(), this::receiveUris);
  private final ActivityResultLauncher<String[]> photos =
      registerForActivityResult(
          new ActivityResultContracts.OpenMultipleDocuments(), this::receiveUris);
  private final ActivityResultLauncher<String> cameraPermission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            if (granted) launchCamera();
            else toast(R.string.chat_camera_denied);
          });
  private final ActivityResultLauncher<Uri> camera =
      registerForActivityResult(
          new ActivityResultContracts.TakePicture(),
          saved -> {
            File file = cameraFile();
            if (file != null) {
              if (saved) {
                pendingCamera = true;
                drainImports();
                return;
              } else file.delete();
            }
            cameraName = null;
            cameraThread = null;
          });

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      initialChatApplied = state.getBoolean("initial_chat_applied");
      cameraName = state.getString("camera_name");
      cameraThread = state.getString("camera_thread");
      pendingCamera = state.getBoolean("pending_camera");
      ArrayList<Uri> saved = state.getParcelableArrayList("pending_uris");
      if (saved != null) pendingUris = saved;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
    model = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
    root =
        com.example.asm_final.ui.ChatUi.view(
            requireContext(),
            model,
            new com.example.asm_final.ui.ChatUi.Actions() {
              public void history() {
                if (getChildFragmentManager().findFragmentByTag("chat_history") == null)
                  new ChatHistoryDialog().show(getChildFragmentManager(), "chat_history");
              }

              public void options() {
                if (model.snapshot != null)
                  ChatMenus.thread(requireContext(), root, model, model.snapshot.thread);
              }

              public void grade() {
                chooseGrade();
              }

              public void attach() {
                attachments();
              }

              public void file(ChatRecords.FileItem file) {
                openFile(file);
              }

              public void copy(String text) {
                ((android.content.ClipboardManager)
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("AI Mentor", text));
                toast(R.string.chat_copied);
              }

              public void formula(String text) {
                showFormula(text);
              }

              public void returnToLesson() {
                model.flush();
                requireActivity().finish();
              }
            },
            requireActivity().getIntent().getBooleanExtra("LESSON_RETURN", false));
    return root;
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    model
        .changes()
        .observe(
            getViewLifecycleOwner(),
            ignored -> {
              if (!initialChatApplied && !model.loading && model.snapshot != null) {
                String target = requireActivity().getIntent().getStringExtra("CHAT_THREAD_ID");
                initialChatApplied = true;
                if (target != null && !target.equals(model.threadId())) {
                  model.select(target);
                  return;
                }
              }
              drainImports();
            });
    model.bind(ActiveUser.get(requireContext()));
  }

  private void showFormula(String text) {
    android.webkit.WebView web = new android.webkit.WebView(requireContext());
    com.example.mentor.ui.MathTextRenderer.render(
        web,
        text.replace("\\(", "$").replace("\\)", "$").replace("\\[", "$$").replace("\\]", "$$"),
        16);
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_formula)
            .setView(web)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    dialog.setOnDismissListener(d -> web.destroy());
    dialog.show();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (model != null && model.threadId() != null) model.refresh();
  }

  @Override
  public void onPause() {
    if (model != null) model.flush();
    super.onPause();
  }

  private void receiveUris(List<Uri> uris) {
    pendingUris.addAll(uris);
    drainImports();
  }

  private void drainImports() {
    if (model == null || model.snapshot == null || model.loading || model.importing || root == null)
      return;
    if (pendingCamera) {
      File file = cameraFile();
      String target = cameraThread;
      pendingCamera = false;
      cameraName = null;
      cameraThread = null;
      if (file != null) model.importCamera(file, target);
    } else if (!pendingUris.isEmpty()) {
      List<Uri> selected = new ArrayList<>(pendingUris);
      pendingUris.clear();
      model.importUris(selected);
    }
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private void chooseGrade() {
    String[] labels = {
      getString(R.string.chat_grade, 10),
      getString(R.string.chat_grade, 11),
      getString(R.string.chat_grade, 12)
    };
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.chat_choose_grade)
        .setSingleChoiceItems(
            labels,
            model.grade >= 10 && model.grade <= 12 ? model.grade - 10 : -1,
            (dialog, which) -> {
              model.setGrade(which + 10);
              dialog.dismiss();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void attachments() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.chat_attach)
        .setItems(
            new String[] {
              getString(R.string.chat_photos),
              getString(R.string.chat_camera),
              getString(R.string.chat_documents)
            },
            (d, which) -> {
              if (which == 0) photos.launch(new String[] {"image/jpeg", "image/png", "image/webp"});
              else if (which == 2)
                documents.launch(
                    new String[] {
                      "application/pdf", "text/plain", "image/jpeg", "image/png", "image/webp"
                    });
              else if (ContextCompat.checkSelfPermission(
                      requireContext(), Manifest.permission.CAMERA)
                  == PackageManager.PERMISSION_GRANTED) launchCamera();
              else cameraPermission.launch(Manifest.permission.CAMERA);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private File cameraFile() {
    if (cameraName == null || !cameraName.matches("[0-9a-fA-F-]{36}\\.jpg") || !isAdded())
      return null;
    return new File(new File(requireContext().getCacheDir(), "general_chat_camera"), cameraName);
  }

  private void launchCamera() {
    if (model == null || model.snapshot == null || model.loading || model.importing) return;
    try {
      File directory = new File(requireContext().getCacheDir(), "general_chat_camera");
      if (!directory.isDirectory() && !directory.mkdirs())
        throw new IOException("Camera storage unavailable");
      cameraName = UUID.randomUUID().toString() + ".jpg";
      cameraThread = model.threadId();
      File target = cameraFile();
      if (!target.createNewFile()) throw new IOException("Camera file unavailable");
      Uri uri =
          FileProvider.getUriForFile(
              requireContext(), requireContext().getPackageName() + ".fileprovider", target);
      camera.launch(uri);
    } catch (IOException | RuntimeException invalid) {
      File file = cameraFile();
      if (file != null) file.delete();
      cameraName = null;
      cameraThread = null;
      toast(R.string.chat_camera_unavailable);
    }
  }

  private void openFile(ChatRecords.FileItem file) {
    try {
      File source = new LocalChatFiles(requireContext()).resolve(model.owner(), file);
      if (!source.isFile()) throw new IOException("Missing local attachment");
      Uri uri =
          FileProvider.getUriForFile(
              requireContext(), requireContext().getPackageName() + ".fileprovider", source);
      startActivity(
          new Intent(Intent.ACTION_VIEW)
              .setDataAndType(uri, file.mime)
              .putExtra(Intent.EXTRA_TITLE, file.name)
              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    } catch (IOException | RuntimeException invalid) {
      toast(R.string.chat_file_open_failed);
    }
  }

  private void toast(int message) {
    if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    out.putBoolean("initial_chat_applied", initialChatApplied);
    out.putString("camera_name", cameraName);
    out.putString("camera_thread", cameraThread);
    out.putBoolean("pending_camera", pendingCamera);
    out.putParcelableArrayList("pending_uris", pendingUris);
  }

  @Override
  public void onDestroyView() {
    root = null;
    super.onDestroyView();
  }
}
