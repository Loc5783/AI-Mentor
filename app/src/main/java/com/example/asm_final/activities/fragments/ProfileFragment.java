package com.example.asm_final.activities.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.example.asm_final.R;
import com.example.asm_final.activities.database.DatabaseHelper;

public class ProfileFragment extends Fragment {

    private EditText edtFullName, edtAddress, edtPhone, edtEmail, edtPassword;
    private Button btnUpdate, btnChooseAvatar;
    private ImageView imgAvatar;
    private DatabaseHelper dbHelper;
    private String username;
    private String avatarUri;

    public interface OnProfileUpdatedListener {
        void onProfileUpdated();
    }

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                getContext().getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                            avatarUri = uri.toString();
                            imgAvatar.setImageURI(uri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        dbHelper = new DatabaseHelper(getContext());
        
        if (getArguments() != null) {
            username = getArguments().getString("USERNAME");
        }

        edtFullName = view.findViewById(R.id.edtProfileFullName);
        edtAddress = view.findViewById(R.id.edtProfileAddress);
        edtPhone = view.findViewById(R.id.edtProfilePhone);
        edtEmail = view.findViewById(R.id.edtProfileEmail);
        edtPassword = view.findViewById(R.id.edtProfilePassword);
        btnUpdate = view.findViewById(R.id.btnUpdateProfile);
        btnChooseAvatar = view.findViewById(R.id.btnChooseAvatar);
        imgAvatar = view.findViewById(R.id.imgProfileAvatar);

        loadUserData();

        btnChooseAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnUpdate.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (!password.isEmpty() && !isValidPassword(password)) {
                Toast.makeText(getContext(), R.string.reg_msg_weak_pass, Toast.LENGTH_LONG).show();
                return;
            }

            if (dbHelper.updateUser(username, fullName, address, phone, email, password, avatarUri)) {
                edtPassword.setText("");
                Toast.makeText(getContext(), R.string.prof_update_success, Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof OnProfileUpdatedListener) {
                    ((OnProfileUpdatedListener) getActivity()).onProfileUpdated();
                }
            } else {
                Toast.makeText(getContext(), R.string.prof_update_fail, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadUserData() {
        if (username == null) return;
        
        Cursor cursor = dbHelper.getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            int fullNameIdx = cursor.getColumnIndex(DatabaseHelper.COL_FULLNAME);
            int addressIdx = cursor.getColumnIndex(DatabaseHelper.COL_ADDRESS);
            int phoneIdx = cursor.getColumnIndex(DatabaseHelper.COL_PHONE);
            int emailIdx = cursor.getColumnIndex(DatabaseHelper.COL_EMAIL);
            int avatarIdx = cursor.getColumnIndex(DatabaseHelper.COL_AVATAR);

            if (fullNameIdx != -1) edtFullName.setText(cursor.getString(fullNameIdx));
            if (addressIdx != -1) edtAddress.setText(cursor.getString(addressIdx));
            if (phoneIdx != -1) edtPhone.setText(cursor.getString(phoneIdx));
            if (emailIdx != -1) edtEmail.setText(cursor.getString(emailIdx));
            if (avatarIdx != -1) {
                avatarUri = cursor.getString(avatarIdx);
                if (avatarUri != null && !avatarUri.isEmpty()) {
                    try {
                        imgAvatar.setImageURI(Uri.parse(avatarUri));
                    } catch (Exception e) {
                        imgAvatar.setImageResource(R.mipmap.ic_launcher_round);
                    }
                }
            }
            
            cursor.close();
        }
    }
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUppercase = true;
            if (Character.isDigit(ch)) hasDigit = true;
        }
        return hasUppercase && hasDigit;
    }

}