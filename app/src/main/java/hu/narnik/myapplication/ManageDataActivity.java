package hu.narnik.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class ManageDataActivity extends BaseActivity {

    private DatabaseReference productsRef;
    private List<ProductItem> productList = new ArrayList<>();
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_manage_data);

        setToolbarTitle(AppState.currentNoteType, "Új termék");

        EditText productEditText = findViewById(R.id.contactEditText);
        Button btnAddProduct = findViewById(R.id.addContactButton);
        RecyclerView productRecyclerView = findViewById(R.id.partnerRecyclerView);


        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app"
        );

        if ("CSOPORT".equals(AppState.currentNoteType) && AppState.currentGroupId != null) {

            productsRef = db.getReference("group_products").child(AppState.currentGroupId);
        } else {

            productsRef = db.getReference("products").child(uid);
        }
        // Adapter + RecyclerView
        adapter = new ProductAdapter(this, productList, productsRef);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productRecyclerView.setAdapter(adapter);

        // Betöltés Firebase-ből
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.getValue(String.class);
                    String key = ds.getKey();
                    if (name != null && key != null) {
                        productList.add(new ProductItem(key, name));
                    }
                }
                productList.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError error) { }
        });

        // Új termék hozzáadása
        btnAddProduct.setOnClickListener(v -> {
            String newProduct = productEditText.getText().toString().trim();

            if (newProduct.isEmpty()) return;


            boolean exists = false;
            for (ProductItem item : productList) {
                if (item.getName().equalsIgnoreCase(newProduct)) {
                    exists = true;
                    break;
                }
            }

            if (exists) {

                Toast.makeText(ManageDataActivity.this,
                        "Ez a termék már szerepel a listában",
                        Toast.LENGTH_SHORT).show();

                productEditText.setText("");

                return;
            }


            String key = productsRef.push().getKey();
            if (key != null) {
                productsRef.child(key).setValue(newProduct);
                Toast.makeText(ManageDataActivity.this,
                        "Új termék feltöltve",
                        Toast.LENGTH_SHORT).show();
                productEditText.setText("");
            }
        });

    }
}