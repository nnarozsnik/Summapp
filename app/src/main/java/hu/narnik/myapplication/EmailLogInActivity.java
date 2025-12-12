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

public class EmailLogInActivity extends AppCompatActivity {

    private final String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    private EditText emailEditText, passwordEditText;
    private Button okButton;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_email_log_in);


        auth = FirebaseAuth.getInstance();


        usersRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users");

        emailEditText = findViewById(R.id.emailAddressEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        okButton = findViewById(R.id.okButton);

        okButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nincs minden mező kitöltve", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (user.isEmailVerified()) {
                                    saveUserToDatabase(user);
                                    Toast.makeText(this, "Sikeres bejelentkezés: " + user.getEmail(), Toast.LENGTH_SHORT).show();


                                    Intent intent = new Intent(EmailLogInActivity.this, PrivateOrGroup.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Erősítse meg az email címet!", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        Log.e("Auth", "Bejelentkezés sikertelen", task.getException());
                        Toast.makeText(this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

    // Felhasználó mentése a database-be
    private void saveUserToDatabase(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String email = user.getEmail();

        usersRef.child(uid).child("email").setValue(email)
                .addOnSuccessListener(aVoid -> Log.d("UsersDB", "User saved: " + email))
                .addOnFailureListener(e -> Log.e("UsersDB", "Hiba a felhasználó mentésekor: " + e.getMessage()));
    }


    private void registerUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user);
                            Toast.makeText(this, "Sikeres regisztráció: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Auth", "Regisztráció sikertelen", task.getException());
                        Toast.makeText(this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}