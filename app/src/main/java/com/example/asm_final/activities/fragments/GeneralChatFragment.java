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
  private ChatMessagesAdapter adapter;
  private EditText input;
  private RecyclerView messages;
  private ChipGroup draftFiles;
  private View root;
  private boolean rendering;
  private boolean initialChatApplied;
  private String renderedThread, cameraName, cameraThread, filesSignature = "";
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
    return inflater.inflate(R.layout.fragment_general_chat, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    root = view;
    model = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
    input = view.findViewById(R.id.chat_input);
    input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(30000)});
    messages = view.findViewById(R.id.chat_messages);
    messages.setLayoutManager(new LinearLayoutManager(requireContext()));
    draftFiles = view.findViewById(R.id.chat_draft_files);
    adapter =
        new ChatMessagesAdapter(
            ActiveUser.get(requireContext()),
            new ChatMessagesAdapter.Actions() {
              public void retry(String id) {
                model.retry(id);
              }

              public void file(ChatRecords.FileItem file) {
                openFile(file);
              }

              public boolean retryablePending(String id) {
                return model.hasUnsavedReply(id);
              }
            });
    messages.setAdapter(adapter);
    input.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!rendering) model.setDraft(s.toString());
          }

          public void afterTextChanged(Editable editable) {}
        });
    view.findViewById(R.id.chat_history)
        .setOnClickListener(
            v -> {
              if (getChildFragmentManager().findFragmentByTag("chat_history") == null)
                new ChatHistoryDialog().show(getChildFragmentManager(), "chat_history");
            });
    view.findViewById(R.id.chat_new).setOnClickListener(v -> model.newChat());
    view.findViewById(R.id.chat_grade).setOnClickListener(v -> chooseGrade());
    view.findViewById(R.id.chat_options)
        .setOnClickListener(
            v -> {
              if (model.snapshot != null)
                ChatMenus.thread(requireContext(), v, model, model.snapshot.thread);
            });
    view.findViewById(R.id.chat_attach).setOnClickListener(v -> attachments());
    view.findViewById(R.id.chat_send)
        .setOnClickListener(
            v -> {
              if (model.requesting) model.cancel();
              else if (model.grade < 10 || model.grade > 12) chooseGrade();
              else model.send();
            });
    for (int id :
        new int[] {
          R.id.chat_suggest_physics, R.id.chat_suggest_chemistry, R.id.chat_suggest_biology
        })
      view.findViewById(id)
          .setOnClickListener(
              v -> {
                input.setText(
                    v.getId() == R.id.chat_suggest_physics
                        ? R.string.chat_suggest_physics
                        : v.getId() == R.id.chat_suggest_chemistry
                            ? R.string.chat_suggest_chemistry
                            : R.string.chat_suggest_biology);
                input.setSelection(input.length());
                input.requestFocus();
              });
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
              render();
            });
    View backToLesson = view.findViewById(R.id.chat_return_lesson);
    backToLesson.setVisibility(
        requireActivity().getIntent().getBooleanExtra("LESSON_RETURN", false)
            ? View.VISIBLE
            : View.GONE);
    backToLesson.setOnClickListener(
        v -> {
          model.flush();
          requireActivity().finish();
        });
    model.bind(ActiveUser.get(requireContext()));
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

  private void render() {
    if (root == null) return;
    rendering = true;
    try {
      if (!input.getText().toString().equals(model.draft)) {
        int caret = input.length() == 0 ? model.draft.length() : input.getSelectionStart();
        input.setText(model.draft);
        input.setSelection(
            Math.max(0, Math.min(caret < 0 ? model.draft.length() : caret, model.draft.length())));
      }
      input.setEnabled(!model.loading && !model.importing);
      boolean ready = model.snapshot != null && !model.loading && !model.importing;
      for (int id :
          new int[] {
            R.id.chat_new,
            R.id.chat_history,
            R.id.chat_attach,
            R.id.chat_options,
            R.id.chat_grade,
            R.id.chat_suggest_physics,
            R.id.chat_suggest_chemistry,
            R.id.chat_suggest_biology
          }) root.findViewById(id).setEnabled(ready);
      TextView grade = root.findViewById(R.id.chat_grade);
      grade.setText(
          model.grade >= 10 && model.grade <= 12
              ? getString(R.string.chat_grade, model.grade)
              : getString(R.string.chat_choose_grade));
      ((TextView) root.findViewById(R.id.chat_title))
          .setText(
              model.snapshot == null || model.snapshot.thread.title.isEmpty()
                  ? getString(R.string.chat_title)
                  : model.snapshot.thread.title);
      root.findViewById(R.id.chat_empty)
          .setVisibility(
              model.snapshot != null && model.snapshot.messages.isEmpty()
                  ? View.VISIBLE
                  : View.GONE);
      root.findViewById(R.id.chat_loading)
          .setVisibility(model.loading || model.importing ? View.VISIBLE : View.GONE);
      int status =
          model.status != 0
              ? model.status
              : model.importing
                  ? R.string.chat_importing
                  : model.requesting
                      ? model.busyHere() ? R.string.chat_thinking : R.string.chat_thinking_other
                      : 0;
      TextView statusView = root.findViewById(R.id.chat_status);
      statusView.setVisibility(status == 0 ? View.GONE : View.VISIBLE);
      if (status != 0) statusView.setText(status);
      ImageButton send = root.findViewById(R.id.chat_send);
      send.setImageResource(model.requesting ? R.drawable.ic_chat_stop : R.drawable.ic_chat_send);
      send.setContentDescription(
          getString(model.requesting ? R.string.chat_stop : R.string.chat_send));
      send.setEnabled(ready);
      List<ChatRecords.Message> rows =
          model.snapshot == null ? Collections.emptyList() : model.snapshot.messages;
      LinearLayoutManager layout = (LinearLayoutManager) messages.getLayoutManager();
      boolean switched = !Objects.equals(renderedThread, model.threadId());
      boolean atBottom = layout.findLastVisibleItemPosition() >= adapter.getItemCount() - 3;
      renderedThread = model.threadId();
      adapter.setRequesting(model.requesting);
      adapter.submitList(
          rows,
          () -> {
            if (root != null && !rows.isEmpty() && (switched || atBottom))
              messages.scrollToPosition(rows.size() - 1);
          });
      StringBuilder signature = new StringBuilder();
      if (model.snapshot != null)
        for (ChatRecords.FileItem f : model.snapshot.draftFiles) signature.append(f.id);
      String key = signature.toString() + ":" + model.importing + ":" + model.loading;
      if (!key.equals(filesSignature)) {
        filesSignature = key;
        draftFiles.removeAllViews();
        if (model.snapshot != null)
          for (ChatRecords.FileItem file : model.snapshot.draftFiles) {
            Chip chip = new Chip(requireContext());
            chip.setText(file.name);
            chip.setMaxWidth(dp(250));
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.chat_remove_file, file.name));
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnClickListener(v -> openFile(file));
            chip.setOnCloseIconClickListener(v -> model.removeFile(file.id));
            chip.setEnabled(ready);
            draftFiles.addView(chip);
          }
        draftFiles.setVisibility(draftFiles.getChildCount() == 0 ? View.GONE : View.VISIBLE);
      }
    } finally {
      rendering = false;
    }
    drainImports();
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
    if (messages != null) messages.setAdapter(null);
    if (adapter != null) adapter.close();
    root = null;
    input = null;
    messages = null;
    draftFiles = null;
    adapter = null;
    filesSignature = "";
    renderedThread = null;
    super.onDestroyView();
  }
}
