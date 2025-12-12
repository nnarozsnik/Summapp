package hu.narnik.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class NewNotes extends BaseActivity {

    private EditText noteEditText;
    private String noteId;
    private boolean isGroupMode;
    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";
    private DatabaseReference notesRef;
    private String groupId;
    private EditText editTextText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_new_notes);
        setToolbarTitle(AppState.currentNoteType, "Jegyzet");

        editTextText = findViewById(R.id.editTextText);

        noteEditText = findViewById(R.id.editTextTextMultiLine);

        Log.d("NewNotes", "isGroupNote: " + getIntent().getBooleanExtra("isGroupNote", false));
        Log.d("NewNotes", "groupId: " + getIntent().getStringExtra("groupId"));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNote();
                finish();
            }
        });


        noteId = getIntent().getStringExtra("noteId");
        isGroupMode = "CSOPORT".equals(AppState.currentNoteType);
        groupId = AppState.currentGroupId;

        if (isGroupMode && groupId != null && !groupId.isEmpty()) {
            notesRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("groups")
                    .child(groupId)
                    .child("items");
        } else {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            notesRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("users")
                    .child(uid)
                    .child("notes");
        }


        findViewById(R.id.saveNoteButton).setOnClickListener(v -> {
            saveNote();
            finish();
        });


        if (noteId != null) {
            loadNote();
        }

        Button deleteButton = findViewById(R.id.deleteButton);

        deleteButton.setOnClickListener(v -> {

            noteEditText.setText("");
            editTextText.setText("");


            if (noteId != null) {
                notesRef.child(noteId).removeValue();
                Toast.makeText(this, "Jegyzet törölve", Toast.LENGTH_SHORT).show();
            }


            finish();
        });
    }



    private void loadNote() {
        notesRef.child(noteId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String content = s.child("content").getValue(String.class);
                if (content != null) noteEditText.setText(content);

                String title = s.child("title").getValue(String.class);
                if (title != null) editTextText.setText(title);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveNote() {
        String content = noteEditText.getText().toString();
        String titleInput = editTextText.getText().toString().trim();

        if (content.trim().isEmpty()) return;

        boolean isNew = noteId == null;
        if (isNew) noteId = notesRef.push().getKey();

        String title = !titleInput.isEmpty() ? titleInput :
                (content.contains("\n") ? content.substring(0, content.indexOf("\n")) : content);
        if (title.isEmpty()) title = "(Névtelen)";

        long timestamp = System.currentTimeMillis();

        notesRef.child(noteId).child("title").setValue(title);
        notesRef.child(noteId).child("content").setValue(content);


        if (isNew) {
            notesRef.child(noteId).child("createdAt").setValue(timestamp);

            String author = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (author == null) author = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            notesRef.child(noteId).child("author").setValue(author);
        }
    }
}