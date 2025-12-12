package hu.narnik.myapplication;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteVH> {

    private List<Note> notes;
    private OnNoteClickListener clickListener;
    private OnNoteLongClickListener longClickListener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note, int position);
    }

    public NotesAdapter(List<Note> notes,boolean isGroupNote, OnNoteClickListener listener) {
        this.notes = notes;
        this.isGroupNote = isGroupNote;
        this.clickListener = listener;
    }

    public void setOnNoteLongClickListener(OnNoteLongClickListener listener) {
        this.longClickListener = listener;
    }

    private boolean isGroupNote;

    @NonNull
    @Override
    public NoteVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteVH holder, int position) {
        Note note = notes.get(position);
        holder.title.setText(note.getTitle());
        holder.preview.setText(note.getContent());
        holder.author.setText(note.getAuthor());

        holder.itemView.setOnClickListener(v -> clickListener.onNoteClick(note));


        if (note.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(note.getCreatedAt()));
            holder.date.setText(dateStr);
        } else {
            holder.date.setText("");
        }

        if (isGroupNote && note.getAuthor() != null) {
            holder.author.setVisibility(View.VISIBLE);
            holder.author.setText(note.getAuthor());
        } else {
            holder.author.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteVH extends RecyclerView.ViewHolder {
        TextView title, preview, date, author;

        NoteVH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitleText);
            preview = itemView.findViewById(R.id.notePreviewText);
            date = itemView.findViewById(R.id.noteDateText);
            author = itemView.findViewById(R.id.noteAuthorText);
        }
    }
}