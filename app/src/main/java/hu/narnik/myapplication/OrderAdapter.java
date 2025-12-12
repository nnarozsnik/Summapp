package hu.narnik.myapplication;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {


    public interface OnDataChangedListener {
        void onDataChanged();

    }


    private OnDataChangedListener listener;

    public OrderAdapter(Context context, List<OrderProduct> orderList,
                        DatabaseReference ordersRef, DatabaseReference productsRef,
                        OnDataChangedListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.ordersRef = ordersRef;
        this.productsRef = productsRef;
        this.listener = listener;
    }

    private final String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";
    private List<OrderProduct> orderList;
    private Context context;
    private DatabaseReference ordersRef;
    private DatabaseReference productsRef;




    public List<OrderProduct> getOrderList() {
        return orderList;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartner, tvProduct, tvQuantity, tvDate;

        public ViewHolder(View itemView) {
            super(itemView);
            tvPartner = itemView.findViewById(R.id.tvPartner);
            tvProduct = itemView.findViewById(R.id.tvProduct);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.order_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderProduct item = orderList.get(position);
        holder.tvPartner.setText(item.getPartner());
        holder.tvProduct.setText(item.getProduct());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvDate.setText(item.getDate());

        holder.itemView.setOnLongClickListener(v -> {
            showEditDeleteDialog(item, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    // ----------------- Edit/Delete Dialog -----------------
    private void showEditDeleteDialog(OrderProduct item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);




        builder.setTitle("Művelet");
        builder.setItems(new CharSequence[]{"Szerkesztés", "Törlés"}, (dialog, which) -> {
            switch (which) {
                case 0: showEditDialog(item, position); break;
                case 1: confirmDelete(item, position); break;
            }
        });
        builder.show();

    }

    // ----------------- Delete Item -----------------
    private void confirmDelete(OrderProduct item, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Törlés megerősítése")
                .setMessage("Biztosan törölni szeretné ezt a rendelést?")
                .setPositiveButton("Törlés", (dialog, which) -> deleteItem(item, position))
                .setNegativeButton("Mégse", null)
                .show();
    }

    private void deleteItem(OrderProduct item, int position) {
        if (item.getId() != null) {
            ordersRef.child(item.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {

                        Toast.makeText(context, "Rendelés törölve", Toast.LENGTH_SHORT).show();




                      /*
                        if (context instanceof OrdersActivity) {
                            ((OrdersActivity) context).updateSummaryFromAdapter();
                        }
                    })*/


                        if (listener != null) {
                            listener.onDataChanged();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Törlés sikertelen", Toast.LENGTH_SHORT).show()
                    );

        }
    }

    // ----------------- Edit Item -----------------
    private void showEditDialog(OrderProduct item, int position) {
        View editView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_order, null);



        DatabaseReference productsRef = this.productsRef;
        if ("CSOPORT".equals(AppState.currentNoteType)) {
            productsRef = FirebaseDatabase.getInstance().getReference("group_products");
        } else {
            productsRef = FirebaseDatabase.getInstance().getReference("products");
        }

        EditText partnerET = editView.findViewById(R.id.editPartner);
        EditText productET = editView.findViewById(R.id.editProduct);
        EditText quantityET = editView.findViewById(R.id.editQuantity);
        EditText dateET = editView.findViewById(R.id.editDate);


        TextView addedTextView = editView.findViewById(R.id.addedTextView);
        TextView addedLabel = editView.findViewById(R.id.textView13);

        addedTextView.setVisibility(View.GONE);
        addedLabel.setVisibility(View.GONE);


        if ("CSOPORT".equals(AppState.currentNoteType)) {
            String createdBy = (item.getAuthor() != null && !item.getAuthor().isEmpty())
                    ? item.getAuthor()
                    : "Ismeretlen";
            addedTextView.setText(createdBy);
            addedTextView.setVisibility(View.VISIBLE);
            addedLabel.setVisibility(View.VISIBLE);
        }

        partnerET.setText(item.getPartner());
        productET.setText(item.getProduct());
        quantityET.setText(String.valueOf(item.getQuantity()));
        dateET.setText(item.getDate());

        // DatePicker a szerkesztéshez
        dateET.setFocusable(false);
        dateET.setClickable(true);
        dateET.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            new DatePickerDialog(context,
                    (view, year, month, dayOfMonth) -> dateET.setText(String.format("%02d.%02d", month + 1, dayOfMonth)),
                    today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rendelés szerkesztése");
        builder.setView(editView);


        builder.setPositiveButton("Mentés", (dialog, which) -> {
            String newPartner = partnerET.getText().toString().trim();
            String newProduct = productET.getText().toString().trim();
            final String editedProduct = newProduct;

            int newQuantity = 0;
            try {
                newQuantity = Integer.parseInt(quantityET.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            item.setPartner(newPartner);
            item.setProduct(newProduct);
            item.setQuantity(newQuantity);
            item.setDate(dateET.getText().toString().trim());

            if (item.getId() != null) {
                ordersRef.child(item.getId()).setValue(item)
                        .addOnSuccessListener(aVoid -> {
                            notifyItemChanged(position);
                            Toast.makeText(context, "Rendelés frissítve", Toast.LENGTH_SHORT).show();

                            if (listener != null) listener.onDataChanged();

                            // --- Termék feltöltés, ha nem létezik ---
                            if (!editedProduct.isEmpty()) {
                                final DatabaseReference productsRefFinal = this.productsRef;
                                productsRefFinal.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        boolean exists = false;
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            String existing = ds.getValue(String.class);
                                            if (existing != null && existing.equalsIgnoreCase(editedProduct)) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            String key = productsRefFinal.push().getKey();
                                            if (key != null) {
                                                productsRefFinal.child(key).setValue(editedProduct);
                                                    //    .addOnSuccessListener(aVoid2 -> {
                                                      //      if (listener != null) listener.onNewProductAdded(editedProduct);
                                                       // });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                });
                            }


                            if (context instanceof OrdersActivity) {
                                ((OrdersActivity) context).updateSummaryFromAdapter();
                            } else if (context instanceof ContactsActivity) {
                                ((ContactsActivity) context).updateSummary();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Frissítés sikertelen", Toast.LENGTH_SHORT).show()
                        );
            }
        });
        builder.setNegativeButton("Mégse", null);
        builder.show();
    }
}