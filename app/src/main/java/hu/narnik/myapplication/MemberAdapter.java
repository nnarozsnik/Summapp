package hu.narnik.myapplication;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<String> memberEmails;

    public MemberAdapter(List<String> memberEmails) {
        this.memberEmails = memberEmails;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new MemberViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String email = memberEmails.get(position);
        holder.emailTextView.setText(email);
    }

    @Override
    public int getItemCount() {
        return memberEmails.size();
    }

    public void setMemberEmails(List<String> emails) {
        this.memberEmails = emails;
        notifyDataSetChanged();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
