package hu.narnik.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<ProductItem> productList;
    private Context context;
    private DatabaseReference productsRef;

    public ProductAdapter(Context context, List<ProductItem> productList, DatabaseReference ref) {
        this.context = context;
        this.productList = productList;
        this.productsRef = ref;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProduct;
        public ViewHolder(View itemView) {
            super(itemView);
            tvProduct = itemView.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = productList.get(position);
        holder.tvProduct.setText(item.getName());

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Törlés")
                    .setMessage("Biztosan törölni szeretné ezt a terméket?")
                    .setPositiveButton("Törlés", (dialog, which) -> {
                        if (item.getId() != null) {
                            productsRef.child(item.getId()).removeValue()
                                    .addOnSuccessListener(aVoid -> {

                                        Toast.makeText(context, "Termék törölve", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Törlés sikertelen", Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .setNegativeButton("Mégse", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}