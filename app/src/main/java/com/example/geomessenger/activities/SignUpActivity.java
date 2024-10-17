package com.example.geomessenger.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geomessenger.R;
import com.example.geomessenger.utilities.Constants;
import com.example.geomessenger.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners(){
        findViewById(R.id.textSignIn).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.buttonSignUp).setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        findViewById(R.id.layoutImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);

        TextView textView;
        textView = findViewById(R.id.inputName);
        String inputName = textView.getText().toString();
        textView = findViewById(R.id.inputEmail);
        String inputEmail = textView.getText().toString();
        textView = findViewById(R.id.inputPassword);
        String inputPassword = textView.getText().toString();

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, inputName);
        user.put(Constants.KEY_EMAIL, inputEmail);
        user.put(Constants.KEY_PASSWORD, inputPassword);
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, inputName);
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private String encodedImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            ImageView imageProfile = findViewById(R.id.imageProfile);
                            imageProfile.setImageBitmap(bitmap);
                            findViewById(R.id.textAddImage).setVisibility(View.GONE);
                            encodedImage = encodedImage(bitmap);
                        } catch (FileNotFoundException e){
                            e.printStackTrace();;
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails(){
        TextView inputName = findViewById(R.id.inputName);
        TextView inputEmail = findViewById(R.id.inputEmail);
        TextView inputPassword = findViewById(R.id.inputPassword);
        TextView inputConfirmPas = findViewById(R.id.inputConfirmPassword);
        if (encodedImage == null){
            showToast("Select profile image");
            return false;
        } else if (inputName.getText().toString().trim().isEmpty()){
            showToast("Enter name");
            return false;
        } else if (inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()){
            showToast("Enter valid email");
            return false;
        } else if (inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter password");
            return false;
        } else if (inputConfirmPas.getText().toString().trim().isEmpty()){
            showToast("Confirm your password");
            return false;
        } else if (!(inputPassword.getText().toString().equals(inputConfirmPas.getText().toString()))){
            showToast("Password and confirm password must be same");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading){
        Button buttonSignUp = findViewById(R.id.buttonSignUp);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (isLoading){
            buttonSignUp.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}