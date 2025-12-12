package hu.narnik.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NewGroup extends AppCompatActivity {

    private EditText groupPwEditTextTextPassword;
    private EditText groupPwAgainEditTextTextPassword;
    private Button okButton;

    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_new_group);


        groupPwEditTextTextPassword = findViewById(R.id.groupPwEditTextTextPassword);
        groupPwAgainEditTextTextPassword = findViewById(R.id.groupPwAgainEditTextTextPassword);
        okButton = findViewById(R.id.okButton);

        okButton.setOnClickListener(v -> createGroup());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Intent intent = new Intent(NewGroup.this, GroupSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });


    }

    private void createGroup() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail();
        String groupId1 = groupPwEditTextTextPassword.getText().toString().trim();
        String groupId2 = groupPwAgainEditTextTextPassword.getText().toString().trim();

        if (groupId1.isEmpty() || groupId2.isEmpty()) {
            Toast.makeText(this, "Nincs minden mező kitöltve", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!groupId1.equals(groupId2)) {
            Toast.makeText(this, "A két azonosító nem egyezik!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupId1.matches(".*[.#$\\[\\]/].*")) {
            Toast.makeText(this, "A csoportazonosító nem tartalmazhat '.', '#', '$', '[', ']' vagy '/' karaktereket!", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference groupsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("groups");


        groupsRef.child(groupId1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(NewGroup.this, "Ez az azonosító már foglalt", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference groupRef = groupsRef.child(groupId1);


                    groupRef.child("admin").child("uid").setValue(uid)
                            .addOnSuccessListener(aVoid -> {

                                groupRef.child("admin").child("email").setValue(email)
                                        .addOnSuccessListener(aVoid2 -> {

                                            groupRef.child("members").child(uid).child("email").setValue(email)
                                                    .addOnSuccessListener(aVoid3 -> {
                                                        Toast.makeText(NewGroup.this, "Csoport létrehozva adminként", Toast.LENGTH_SHORT).show();

                                                        AppState.currentNoteType = "CSOPORT";
                                                        AppState.isGroupView = true;
                                                        AppState.currentGroupId = groupId1;

                                                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                                                .edit()
                                                                .putString("lastGroupId", groupId1)
                                                                .putString("lastNoteType", "CSOPORT")
                                                                .apply();

                                                        Intent intent = new Intent(NewGroup.this, BaseActivity.class);
                                                        intent.putExtra("NOTE_TYPE", "CSOPORT");
                                                        startActivity(intent);
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(NewGroup.this, "Hiba a members létrehozásakor: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                                    );
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(NewGroup.this, "Hiba az admin email mentésekor: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(NewGroup.this, "Hiba az admin UID mentésekor: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NewGroup.this, "Hiba a Firebase lekérdezés során: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}