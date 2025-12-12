package hu.narnik.myapplication;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ContactsActivity extends BaseActivity {

    private AutoCompleteTextView partnerAutoCompleteTextView;
    private RecyclerView partnerRecyclerView;
    private Button dateFilterButton;

    private ArrayAdapter<String> partnerAdapter;
    private List<String> partners;

    private OrderAdapter orderAdapter;
    private List<OrderProduct> privateOrders = new ArrayList<>();
    private List<OrderProduct> groupOrders = new ArrayList<>();
    private List<OrderProduct> filteredOrders = new ArrayList<>();

    private DatabaseReference privateOrdersRef;
    private DatabaseReference productsRef;
    private String uid;
    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    private String startDate = "";
    private String endDate = "";
    private TextView summaryTextView;


    private boolean partnerSortAsc = true;
    private boolean productSortAsc = true;

    private boolean dateSortAsc = true;

    private boolean quantitySortAsc = true;

    private final SimpleDateFormat dateFormatFirebase = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_contacts);


        setToolbarTitle(AppState.currentNoteType, "Vevők");

        summaryTextView = findViewById(R.id.summaryTextView);

        partnerAutoCompleteTextView = findViewById(R.id.partnerAutoCompleteTextView);
        partnerRecyclerView = findViewById(R.id.partnerRecyclerView);
        dateFilterButton = findViewById(R.id.dateFilterButton);


        TextView tvPartnerHeader = findViewById(R.id.tvHeaderPartner);
        TextView tvProductHeader = findViewById(R.id.tvHeaderProduct);
        TextView tvDateHeader = findViewById(R.id.tvHeaderDate);
        TextView tvQuantityHeader = findViewById(R.id.tvHeaderQuantity);


        tvPartnerHeader.setOnClickListener(v -> sortByPartner());
        tvProductHeader.setOnClickListener(v -> sortByProduct());
        tvDateHeader.setOnClickListener(v -> sortByDate());
        tvQuantityHeader.setOnClickListener(v -> sortByQuantity());

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        privateOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("orders").child(uid);

        if (AppState.isGroupView && AppState.currentGroupId != null && !AppState.currentGroupId.isEmpty()) {
            productsRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("group_products").child(AppState.currentGroupId);
        } else {
            productsRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("products").child(uid);
        }

        dateFilterButton.setOnLongClickListener(v -> {
            startDate = "";
            endDate = "";
            applyFilters();


            Toast.makeText(this, "Dátum szűrés törölve", Toast.LENGTH_SHORT).show();

            return true;
        });



        //Partnerek
        partners = new ArrayList<>();
        partnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, partners);
        partnerAutoCompleteTextView.setAdapter(partnerAdapter);
        partnerAutoCompleteTextView.setThreshold(1);

        loadPartnersFromEffectiveRef();

        //  Enter események
        partnerAutoCompleteTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                applyFilters();


                v.clearFocus();

                // Billentyűzet elrejtése
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });




        partnerAutoCompleteTextView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                applyFilters();

                return true;
            }
            return false;
        });

        // --- Adapterek ---
        privateOrders = new ArrayList<>();
        groupOrders = new ArrayList<>();
        filteredOrders = new ArrayList<>();


        filteredOrders = new ArrayList<>();


        DatabaseReference effectiveOrdersRef;

        if (AppState.isGroupView && AppState.currentGroupId != null && !AppState.currentGroupId.isEmpty()) {
            // Csoportos nézet
            effectiveOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("groups")
                    .child(AppState.currentGroupId)
                    .child("orders");
        } else {
            // Privát nézet
            effectiveOrdersRef = privateOrdersRef;
        }

        orderAdapter = new OrderAdapter(
                this,
                filteredOrders,
                effectiveOrdersRef,
                productsRef,
                new OrderAdapter.OnDataChangedListener() {
                    @Override
                    public void onDataChanged() {
                        updateSummary();
                    }

           /*         @Override
                    public void onNewProductAdded(String productName) {
                    } */
                }
        );

        partnerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        partnerRecyclerView.setAdapter(orderAdapter);


        loadAllOrders();

        partnerAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                applyFilters();
            partnerAutoCompleteTextView.clearFocus();


            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(partnerAutoCompleteTextView.getWindowToken(), 0);
            }
        });

        Button partnerOkButton = findViewById(R.id.partnerOkButton);
        partnerOkButton.setOnClickListener(v -> {
                applyFilters();
            partnerAutoCompleteTextView.clearFocus();


            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(partnerAutoCompleteTextView.getWindowToken(), 0);
            }
        });

        dateFilterButton.setOnClickListener(v -> selectDateRange());
    }

    // --- Privát partnerek betöltése ---
    private void loadPrivatePartners() {
        partners.clear();
        privateOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Set<String> partnerSet = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null && order.getPartner() != null) {
                        partnerSet.add(order.getPartner().trim());
                    }
                }
                partners.clear();
                partners.addAll(partnerSet);
                Collections.sort(partners, String.CASE_INSENSITIVE_ORDER);
                partnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Csoportos partnerek betöltése ---
    private void loadGroupPartners() {
        if (AppState.currentGroupId == null || AppState.currentGroupId.isEmpty()) return;

        DatabaseReference groupOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups").child(AppState.currentGroupId).child("orders");

        groupOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Set<String> partnerSet = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null && order.getPartner() != null) {
                        partnerSet.add(order.getPartner().trim());
                    }
                }
                partners.clear();
                partners.addAll(partnerSet);
                Collections.sort(partners, String.CASE_INSENSITIVE_ORDER);
                partnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAllOrders() {
        DatabaseReference effectiveOrdersRef;

        if (AppState.isGroupView && AppState.currentGroupId != null && !AppState.currentGroupId.isEmpty()) {
            effectiveOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("groups")
                    .child(AppState.currentGroupId)
                    .child("orders");
        } else {
            effectiveOrdersRef = privateOrdersRef;
        }

        effectiveOrdersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {


                if (AppState.isGroupView) {
                    groupOrders.clear();
                } else {
                    privateOrders.clear();
                }


                filteredOrders.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null) {
                        if (AppState.isGroupView) {
                            groupOrders.add(order);
                        } else {
                            privateOrders.add(order);
                        }
                        filteredOrders.add(order);
                    }
                }

                orderAdapter.notifyDataSetChanged();
                updateSummary();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadGroupOrders() {
        if (AppState.currentGroupId == null || AppState.currentGroupId.isEmpty()) return;

        DatabaseReference groupOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups").child(AppState.currentGroupId).child("orders");

        groupOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupOrders.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String partner = ds.child("partner").getValue(String.class);
                    String product = ds.child("product").getValue(String.class);
                    String date = ds.child("date").getValue(String.class);
                    Integer quantity = ds.child("quantity").getValue(Integer.class);
                    String author = ds.child("author").getValue(String.class);

                    if (partner != null && product != null && date != null) {
                        OrderProduct order = new OrderProduct();
                        order.setPartner(partner);
                        order.setProduct(product);
                        order.setDate(date);
                        order.setQuantity(quantity);
                        order.setAuthor(author);
                        groupOrders.add(order);
                    }
                }
                applyFilters();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void selectDateRange() {
        Calendar today = Calendar.getInstance();
        Toast.makeText(this, "Válassza ki a kezdő dátumot (-tól)", Toast.LENGTH_SHORT).show();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year, month + 1, dayOfMonth);


            Toast.makeText(this, "Válassza ki a záró dátumot (-ig)", Toast.LENGTH_SHORT).show();
            new DatePickerDialog(this, (view2, year2, month2, day2) -> {
                endDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year2, month2 + 1, day2);


                applyFilters();
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();

        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applyFilters() {
        String selectedPartner = partnerAutoCompleteTextView.getText().toString().trim();
        filteredOrders.clear();

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);


        List<OrderProduct> ordersToFilter;
        if (AppState.isGroupView) {
            ordersToFilter = groupOrders;
        } else {
            ordersToFilter = privateOrders;
        }

        for (OrderProduct order : ordersToFilter) {
            boolean matchesPartner = selectedPartner.isEmpty() ||
                    (order.getPartner() != null && order.getPartner().trim().equalsIgnoreCase(selectedPartner));

            boolean matchesDate = true;
            if (order.getDate() != null && !order.getDate().isEmpty() && (!startDate.isEmpty() || !endDate.isEmpty())) {
                try {
                    String orderDateRaw = order.getDate().split(",")[0].trim();
                    String[] parts = orderDateRaw.split("\\.");
                    String normalizedDate;

                    if (parts.length == 2) {
                        normalizedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                                currentYear, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    } else if (parts.length == 3) {
                        normalizedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                                Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    } else {
                        matchesDate = false;
                        normalizedDate = "";
                    }

                    if (!normalizedDate.isEmpty()) {
                        matchesDate = isDateInRange(normalizedDate, startDate, endDate);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    matchesDate = false;
                }
            }

            if (matchesPartner && matchesDate) {
                filteredOrders.add(order);
            }
        }

        orderAdapter.notifyDataSetChanged();

        updateSummary();
    }

   private void hideKeyboard(View... views) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (View view : views) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                view.clearFocus();
            }
        }
    }


    private void sortByPartner() {
        Collections.sort(filteredOrders, (o1, o2) -> {
            int cmp = o1.getPartner().compareToIgnoreCase(o2.getPartner());
            return partnerSortAsc ? cmp : -cmp;
        });
        partnerSortAsc = !partnerSortAsc;
        orderAdapter.notifyDataSetChanged();
    }

    private void sortByProduct() {
        Collections.sort(filteredOrders, (o1, o2) -> {
            int cmp = o1.getProduct().compareToIgnoreCase(o2.getProduct());
            return productSortAsc ? cmp : -cmp;
        });
        productSortAsc = !productSortAsc;
        orderAdapter.notifyDataSetChanged();
    }

    private void sortByDate() {
        Collections.sort(filteredOrders, (o1, o2) -> {
            int cmp = o1.getDate().compareToIgnoreCase(o2.getDate());
            return dateSortAsc ? cmp : -cmp;
        });
        dateSortAsc = !dateSortAsc;
        orderAdapter.notifyDataSetChanged();
    }

    private void sortByQuantity() {
        Collections.sort(filteredOrders, (o1, o2) -> {
            int cmp = Integer.compare(o1.getQuantity(), o2.getQuantity());
            return quantitySortAsc ? cmp : -cmp;
        });
        quantitySortAsc = !quantitySortAsc;
        orderAdapter.notifyDataSetChanged();
    }


    public void updateSummary() {

        Map<String, Integer> productTotals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (OrderProduct order : filteredOrders) {
            String product = order.getProduct();
            int quantity = order.getQuantity();

            if (product != null) {
                int current = productTotals.getOrDefault(product, 0);
                productTotals.put(product, current + quantity);
            }
        }


        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : productTotals.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" db\n");
        }

        summaryTextView.setText(sb.toString());
    }

    private boolean isDateInRange(String orderDate, String start, String end) {
        try {
            String[] orderParts = orderDate.split("\\.");
            String[] startParts = start.split("\\.");
            String[] endParts = end.split("\\.");

            if (orderParts.length < 3 || startParts.length < 3 || endParts.length < 3) return false;

            int orderVal = Integer.parseInt(orderParts[0]) * 10000 +
                    Integer.parseInt(orderParts[1]) * 100 +
                    Integer.parseInt(orderParts[2]);

            int startVal = Integer.parseInt(startParts[0]) * 10000 +
                    Integer.parseInt(startParts[1]) * 100 +
                    Integer.parseInt(startParts[2]);

            int endVal = Integer.parseInt(endParts[0]) * 10000 +
                    Integer.parseInt(endParts[1]) * 100 +
                    Integer.parseInt(endParts[2]);

            return orderVal >= startVal && orderVal <= endVal;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadPartnersFromEffectiveRef() {
        DatabaseReference effectiveOrdersRef;

        if (AppState.isGroupView && AppState.currentGroupId != null && !AppState.currentGroupId.isEmpty()) {
            effectiveOrdersRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("groups")
                    .child(AppState.currentGroupId)
                    .child("orders");
        } else {
            effectiveOrdersRef = privateOrdersRef;
        }

        effectiveOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> partnerSet = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null && order.getPartner() != null) {
                        partnerSet.add(order.getPartner().trim());
                    }
                }
                partners.clear();
                partners.addAll(partnerSet);
                Collections.sort(partners, String.CASE_INSENSITIVE_ORDER);
                partnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


}