package hu.narnik.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import hu.narnik.myapplication.Note;

public class NotesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<Note> notes = new ArrayList<>();
    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_notes);
        setToolbarTitle(AppState.currentNoteType, "Jegyzetek");

        recyclerView = findViewById(R.id.notesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));



        boolean isGroupMode = "CSOPORT".equals(AppState.currentNoteType);
        adapter = new NotesAdapter(notes, isGroupMode, note -> {

            Intent intent = new Intent(NotesActivity.this, NewNotes.class);
            intent.putExtra("noteId", note.getId());


            intent.putExtra("isGroupNote", getIntent().getBooleanExtra("isGroupNote", false));
            intent.putExtra("groupId", getIntent().getStringExtra("groupId"));

            startActivity(intent);
        });



        adapter.setOnNoteLongClickListener((note, position) -> {
            new androidx.appcompat.app.AlertDialog.Builder(NotesActivity.this)
                    .setTitle("Jegyzet törlése")
                    .setMessage("Biztosan törölni szeretné ezt a jegyzetet?")
                    .setPositiveButton("Törlés", (dialog, which) -> deleteNote(note.getId()))
                    .setNegativeButton("Mégse", null)
                    .show();
        });

        recyclerView.setAdapter(adapter);

        loadNotes();

        Button newNoteButton = findViewById(R.id.notesButton);
        newNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, NewNotes.class);


            intent.putExtra("isGroupNote", getIntent().getBooleanExtra("isGroupNote", false));
            intent.putExtra("groupId", getIntent().getStringExtra("groupId"));

            startActivity(intent);
        });
    }

    private void loadNotes() {
        boolean isGroupMode = "CSOPORT".equals(AppState.currentNoteType);
        String groupId = AppState.currentGroupId;


        DatabaseReference notesRef;
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

        notesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notes.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    String id = s.getKey();
                    String title = s.child("title").getValue(String.class);
                    String content = s.child("content").getValue(String.class);


                    if (title == null || title.isEmpty()) {
                        if (content != null && content.contains("\n")) {
                            title = content.substring(0, content.indexOf("\n"));
                        } else {
                            title = "(Névtelen)";
                        }
                    }

                    long createdAt = 0;
                    if (s.child("createdAt").getValue() != null) {
                        createdAt = s.child("createdAt").getValue(Long.class);
                    }

                    String author = s.child("author").getValue(String.class);
                    notes.add(new Note(id, title, content, createdAt, author));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteNote(String noteId) {
        boolean isGroupMode = "CSOPORT".equals(AppState.currentNoteType);
        String groupId = AppState.currentGroupId;


        DatabaseReference notesRef;
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

        notesRef.child(noteId).removeValue();
        Toast.makeText(this, "Jegyzet törölve", Toast.LENGTH_SHORT).show();
    }
}