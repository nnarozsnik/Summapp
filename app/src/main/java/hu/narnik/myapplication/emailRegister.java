package hu.narnik.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class emailRegister extends AppCompatActivity {

    private final String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    private EditText emailEditText, passwordEditText, passwordConfirmEditText;
    private Button okButton;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_email_register);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users");

        emailEditText = findViewById(R.id.emailRegAddressEditText);
        passwordEditText = findViewById(R.id.passwordRegEditText);
        passwordConfirmEditText = findViewById(R.id.passwordReg2EditText);
        okButton = findViewById(R.id.okRegButton);

        okButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String passwordConfirm = passwordConfirmEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "Nincs minden mező kitöltve", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "A két jelszó nem egyezik!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "A jelszó legalább 6 karakter legyen!", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password);
        });
    }

    private void registerUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user);


                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(this, "Ellenőrző email elküldve!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(this, "Hiba az email küldéskor: " + verifyTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });



                        }
                    } else {
                        Log.e("Auth", "Regisztráció sikertelen", task.getException());
                        Toast.makeText(this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String email = user.getEmail();

        usersRef.child(uid).child("email").setValue(email)
                .addOnSuccessListener(aVoid -> Log.d("UsersDB", "User saved: " + email))
                .addOnFailureListener(e -> Log.e("UsersDB", "Hiba a felhasználó mentésekor: " + e.getMessage()));
    }
}