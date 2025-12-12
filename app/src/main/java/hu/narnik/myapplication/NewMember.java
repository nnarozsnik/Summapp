package hu.narnik.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NewMember extends BaseActivity {

    private EditText newMemberEditText;
    private Button addMemberButton;
    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";
    private String currentGroupId = AppState.currentGroupId;
    private RecyclerView partnerRecyclerView;
    private MemberAdapter adapter;
    private List<String> memberEmails = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_new_member);

        newMemberEditText = findViewById(R.id.newMemberEditText);
        addMemberButton = findViewById(R.id.addMemberButton);
        partnerRecyclerView = findViewById(R.id.partnerRecyclerView);


        partnerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter(memberEmails);
        partnerRecyclerView.setAdapter(adapter);

        addMemberButton.setOnClickListener(v -> addMemberByEmail());


        loadMembers();
    }




    private void addMemberByEmail() {
        String email = newMemberEditText.getText().toString().trim();
        if (email.isEmpty()) return;

        DatabaseReference membersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups")
                .child(currentGroupId)
                .child("members");

        //ideiglenes kulcsot push() segítségével
        String tempKey = membersRef.push().getKey();

        //  email a generált kulcs alá
        if (tempKey != null) {
            membersRef.child(tempKey).child("email").setValue(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Tag hozzáadva", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }

    private void loadMembers() {
        DatabaseReference membersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups")
                .child(currentGroupId)
                .child("members");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberEmails.clear();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String email = memberSnapshot.child("email").getValue(String.class);
                    if (email != null) {
                        memberEmails.add(email);
                    }
                }
                adapter.setMemberEmails(memberEmails);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NewMember.this, "Hiba a tagok betöltésekor: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}











