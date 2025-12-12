package hu.narnik.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
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

public class JoinGroup extends AppCompatActivity {

    private EditText groupIdEditText;
    private Button okButton;
    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_join_group);

        groupIdEditText = findViewById(R.id.pwEditTextTextPassword);
        okButton = findViewById(R.id.okButton);

        okButton.setOnClickListener(v -> {
            String groupId = groupIdEditText.getText().toString().trim();
            joinGroup(groupId);
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // egyedi logika pl. visszalépés MainActivity-re
                Intent intent = new Intent(JoinGroup.this, GroupSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });


    }

    private void joinGroup(String groupId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();
        if (email == null) return;

        final String finalEmail = email.trim();

        DatabaseReference membersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups")
                .child(groupId)
                .child("members");

        Log.d("JoinGroup", "Trying to join group: " + groupId);
        Log.d("JoinGroup", "Current user email: " + finalEmail);

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String memberEmail = memberSnapshot.child("email").getValue(String.class);
                    if (memberEmail != null) {
                        memberEmail = memberEmail.trim();
                        Log.d("JoinGroup", "Member email: " + memberEmail);
                        if (finalEmail.equalsIgnoreCase(memberEmail)) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {

                    AppState.currentGroupId = groupId;
                    AppState.isGroupView = true;


                    getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("lastGroupId", groupId)
                            .putString("lastNoteType", "CSOPORT")
                            .apply();


                    Intent intent = new Intent(JoinGroup.this, BaseActivity.class);
                    intent.putExtra("NOTE_TYPE", "CSOPORT");
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(JoinGroup.this, "Ez az email nincs a csoportban!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("JoinGroup", "Database error: " + error.getMessage());
            }
        });
    }
}