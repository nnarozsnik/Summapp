package hu.narnik.myapplication;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;




public class OrdersActivity extends BaseActivity implements OrderAdapter.OnDataChangedListener {

    @Override
    public void onDataChanged() {
        updateSummaryFromAdapter();
    }
/*
    @Override
    public void onNewProductAdded(String productName) {
        spinnerAdapter.add(productName);
        spinnerAdapter.notifyDataSetChanged();
    }
*/

    private List<OrderProduct> orderList;
    private OrderAdapter adapter;
    private Spinner typeSpinner;
    private ArrayAdapter<String> spinnerAdapter;
    private DatabaseReference productsRef;
    private DatabaseReference ordersRef;
    private String uid;
    private ValueEventListener productsListener;

    private String selectedDate;
    private String startDate;
    private String endDate;

    private List<String> partners;
    private List<String> allPartners;
    private ArrayAdapter<String> partnerAdapter;

    private String newPartner;

    private boolean partnerSortAsc = true;
    private boolean productSortAsc = true;

    private boolean dateSortAsc = true;

    private boolean quantitySortAsc = true;

    private ValueEventListener ordersListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent(R.layout.activity_orders);
        setToolbarTitle(AppState.currentNoteType, "Rendelések");

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";
        FirebaseDatabase db = FirebaseDatabase.getInstance(databaseUrl);

        if ("CSOPORT".equals(AppState.currentNoteType) && AppState.currentGroupId != null) {

            productsRef = db.getReference("group_products").child(AppState.currentGroupId);
            ordersRef = db.getReference("groups").child(AppState.currentGroupId).child("orders");
        } else {

            productsRef = db.getReference("products").child(uid);
            ordersRef = db.getReference("orders").child(uid);
        }

        AutoCompleteTextView partnerTV = findViewById(R.id.partnerTextView);
        EditText quantityET = findViewById(R.id.dbEditTextNumber);
        typeSpinner = findViewById(R.id.itemSpinner);
        EditText dateET = findViewById(R.id.dateEditTextDate);
        Button saveBtn = findViewById(R.id.saveButton);
        RecyclerView orderRecyclerView = findViewById(R.id.orderRecyclerView);
        Button dateRangeButton = findViewById(R.id.dateFilterButton);
        TextView todayTextView = findViewById(R.id.todayTextView);

        TextView tvPartnerHeader = findViewById(R.id.headerPartner);
        TextView tvProductHeader = findViewById(R.id.headerProduct);
        TextView tvDateHeader = findViewById(R.id.headerDate);
        TextView tvQuantityHeader = findViewById(R.id.headerQuantity);

        tvPartnerHeader.setOnClickListener(v -> sortByPartner());
        tvProductHeader.setOnClickListener(v -> sortByProduct());
        tvDateHeader.setOnClickListener(v -> sortByDate());
        tvQuantityHeader.setOnClickListener(v -> sortByQuantity());


        quantityET.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                saveBtn.performClick();


                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                v.clearFocus();
                return true; // esemény feldolgozva
            }
            return false;
        });


        partnerTV.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                quantityET.requestFocus();
                quantityET.setSelection(quantityET.getText().length());


                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(quantityET, InputMethodManager.SHOW_IMPLICIT);
                }

                return true;
            }
            return false;
        });

        partnerTV.setOnItemClickListener((parent, view, position, id) -> {

            quantityET.requestFocus();
            quantityET.setSelection(quantityET.getText().length());


            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(quantityET, InputMethodManager.SHOW_IMPLICIT);
            }
        });



        partners = new ArrayList<>();
        allPartners = new ArrayList<>();

        // --- AutoCompleteTextView partnerek ---

        partnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                partners) {
            @NonNull
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<String> suggestions = new ArrayList<>();

                        if (constraint != null && constraint.length() > 0) {
                            String input = constraint.toString().toLowerCase(Locale.ROOT);
                            for (String partner : allPartners) {
                                if (partner.toLowerCase(Locale.ROOT).startsWith(input)) {
                                    suggestions.add(partner);
                                }
                            }
                        } else {
                            suggestions.addAll(allPartners);
                        }

                        results.values = suggestions;
                        results.count = suggestions.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results != null && results.count > 0) {
                            addAll((List<String>) results.values);
                        }
                        notifyDataSetChanged();
                    }
                };
            }
        };
        partnerTV.setAdapter(partnerAdapter);
        partnerTV.setThreshold(1);

// --- Partner lista betöltése Firebase-ből ---
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allPartners.clear();
                partners.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null) {
                        String newPartner = order.getPartner();
                        boolean exists = false;
                        for (String p : allPartners) {
                            if (p.equalsIgnoreCase(newPartner)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            allPartners.add(newPartner);
                            partners.add(newPartner); // adapterhez
                        }
                    }
                }
                Collections.sort(allPartners, String.CASE_INSENSITIVE_ORDER);


                partners.clear();
                partners.addAll(allPartners);
                partnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        String[] weekdays = {"Vasárnap", "Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat"};

        // --- Mai nap kiírása ---
        Calendar currentDay = Calendar.getInstance();
        String weekdayName = weekdays[currentDay.get(Calendar.DAY_OF_WEEK)-1];
        todayTextView.setText(String.format(Locale.getDefault(), "%02d.%02d, %s",
                currentDay.get(Calendar.MONTH) + 1,
                currentDay.get(Calendar.DAY_OF_MONTH),
                weekdayName));

        // --- Alapértelmezett piacos nap ---
        Calendar suggested = calculateNextMarketDay(Calendar.getInstance());
        selectedDate = formatDateKey(suggested);
        dateET.setText(formatDateWithWeekday(suggested, weekdays));


        // --- RecyclerView setup ---
        // RecyclerView setup
        orderList = new ArrayList<>();

        adapter = new OrderAdapter(
                this,
                orderList,
                ordersRef,
                productsRef,
                new OrderAdapter.OnDataChangedListener() {
                    @Override
                    public void onDataChanged() {
                        updateSummaryFromAdapter();
                    }

               /*     @Override
                    public void onNewProductAdded(String productName) {
                        spinnerAdapter.add(productName);
                        spinnerAdapter.notifyDataSetChanged();
                    } */
                }
        );


        orderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderRecyclerView.setAdapter(adapter);

        // --- Spinner adapter ---
        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(spinnerAdapter);

        productsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> products = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.getValue(String.class);
                    if (name != null) {
                        products.add(name);
                    }
                }
                Collections.sort(products, String.CASE_INSENSITIVE_ORDER);

                spinnerAdapter.clear();
                spinnerAdapter.addAll(products);
                spinnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrdersActivity.this, "Nem sikerült betölteni a termékeket: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        productsRef.addValueEventListener(productsListener);

        // --- Alapértelmezett szűrés a legközelebbi piacos napra ---
       // loadOrdersForDate(selectedDate);

        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null && selectedDate.equals(order.getDate())) {
                        orderList.add(order);
                    }
                }
                Collections.sort(orderList, (o1, o2) ->
                        o1.getPartner().compareToIgnoreCase(o2.getPartner())
                );
                adapter.notifyDataSetChanged();
                updateSummary(orderList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrdersActivity.this, "Hiba a rendelés szinkronizálása közben", Toast.LENGTH_SHORT).show();
            }
        };


        ordersRef.addValueEventListener(ordersListener);

        // --- DatePicker ---
        dateET.setText(selectedDate);
        dateET.setFocusable(false);
        dateET.setClickable(true);
        dateET.setOnClickListener(v -> {
            Calendar todayCal = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    OrdersActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar orderDateCal = Calendar.getInstance();
                        orderDateCal.set(year, month, dayOfMonth);
                        selectedDate = formatDateKey(orderDateCal);
                        dateET.setText(formatDateWithWeekday(orderDateCal, weekdays));
                    },

                    todayCal.get(Calendar.YEAR),
                    todayCal.get(Calendar.MONTH),
                    todayCal.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        // --- Mentés ---
        saveBtn.setOnClickListener(v -> {
            String partner = partnerTV.getText().toString().trim();
            String product = (typeSpinner.getSelectedItem() != null) ? typeSpinner.getSelectedItem().toString() : "";
            int quantity = 0;
            try {
                quantity = Integer.parseInt(quantityET.getText().toString().trim());
            } catch (NumberFormatException e) {
            }

            if (partner.isEmpty() || product.isEmpty()) return;

            // Új partner hozzáadása az allPartners listához (ha még nincs)
            boolean exists = false;
            for (String p : allPartners) {
                if (p.equalsIgnoreCase(partner)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                allPartners.add(partner);
            }


            if (!partners.contains(partner)) {
                partners.add(partner);
             //   partners.add(newPartner);
                partnerAdapter.notifyDataSetChanged();
            }
            String orderDate = selectedDate;

            OrderProduct newItem = new OrderProduct(partner, product, quantity, orderDate);

            String authorName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (authorName == null || authorName.isEmpty()) {

                authorName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
            newItem.setAuthor(authorName);


            String key = ordersRef.push().getKey();
            if (key != null) {
                newItem.setId(key);
                ordersRef.child(key).setValue(newItem).addOnSuccessListener(aVoid -> {


                   // loadOrdersForDate(selectedDate);


                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(partnerTV.getWindowToken(), 0);
                        imm.hideSoftInputFromWindow(quantityET.getWindowToken(), 0);
                    }


                    partnerTV.clearFocus();
                    quantityET.clearFocus();
                });
            }

            partnerTV.setText("");
            quantityET.setText("");
            dateET.setText(formatDateWithWeekday(suggested, weekdays));

        });

        // --- Tartományos szűrés ---
        dateRangeButton.setOnClickListener(v -> {
            Calendar todayCal = Calendar.getInstance();
            Toast.makeText(this, "Válassza ki a kezdő dátumot (-tól)", Toast.LENGTH_SHORT).show();

            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year, month + 1, dayOfMonth);

                Toast.makeText(this, "Válassza ki a záró dátumot (-ig)", Toast.LENGTH_SHORT).show();
                new DatePickerDialog(this, (view2, year2, month2, day2) -> {
                    endDate = String.format(Locale.getDefault(), "%04d.%02d.%02d", year2, month2 + 1, day2);
                    loadOrdersForDateRange(startDate, endDate);
                }, todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH)).show();

            }, todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        dateRangeButton.setOnLongClickListener(v -> {
            startDate = "";
            endDate = "";

            Calendar nextMarketDay = calculateNextMarketDay(Calendar.getInstance());
            selectedDate = formatDateKey(nextMarketDay);

            dateET.setText(formatDateWithWeekday(nextMarketDay, weekdays));

            loadOrdersForDate(selectedDate);

            Toast.makeText(this, "Dátum szűrés törölve\nBetöltve: következő piac", Toast.LENGTH_SHORT).show();
            return true;
        });


    }

    // --- Következő piacos nap ---
    private Calendar calculateNextMarketDay(Calendar today) {
        Calendar suggested = (Calendar) today.clone();
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.MONDAY: // hétfő → kedd
                suggested.add(Calendar.DAY_OF_MONTH, Calendar.TUESDAY - dayOfWeek);
                break;

            case Calendar.TUESDAY: // kedd → csütörtök
            case Calendar.WEDNESDAY:
                suggested.add(Calendar.DAY_OF_MONTH, Calendar.THURSDAY - dayOfWeek);
                break;

            case Calendar.THURSDAY: // csütörtök → vasárnap
            case Calendar.FRIDAY:
            case Calendar.SATURDAY:
                suggested.add(Calendar.DAY_OF_MONTH, (Calendar.SUNDAY + 7) - dayOfWeek);
                break;

            case Calendar.SUNDAY: // vasárnap → következő kedd
                suggested.add(Calendar.DAY_OF_MONTH, (Calendar.TUESDAY ) - dayOfWeek);
                break;
        }

        return suggested;
    }
    // --- Dátum formázás ---
    private String formatDateWithWeekday(Calendar cal, String[] weekdays) {
        return String.format(Locale.getDefault(), "%02d.%02d, %s",
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]);
    }

    // --- Egy napos szűrés ---
    private void loadOrdersForDate(String date) {
        ordersRef.orderByChild("date").equalTo(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            OrderProduct order = ds.getValue(OrderProduct.class);
                            if (order != null) orderList.add(order);
                        }
                        Collections.sort(orderList, (o1, o2) ->
                                o1.getPartner().compareToIgnoreCase(o2.getPartner())
                        );
                        adapter.notifyDataSetChanged();
                        updateSummary(orderList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrdersActivity.this, "Hiba a lekérdezéskor", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // --- Tartományos szűrés ---
    private void loadOrdersForDateRange(String startDate, String endDate) {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    OrderProduct order = ds.getValue(OrderProduct.class);
                    if (order != null) {
                        String orderDateRaw = order.getDate().split(",")[0].trim();

                        // --- Régi formátum javítása (ha nincs benne év) ---
                        String[] parts = orderDateRaw.split("\\.");
                        String normalizedDate;
                        if (parts.length == 2) {
                            // nincs év, tegyük hozzá az aktuálisat
                            normalizedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                                    currentYear,
                                    Integer.parseInt(parts[0]),
                                    Integer.parseInt(parts[1]));
                        } else if (parts.length == 3) {

                            normalizedDate = String.format(Locale.getDefault(), "%04d.%02d.%02d",
                                    Integer.parseInt(parts[0]),
                                    Integer.parseInt(parts[1]),
                                    Integer.parseInt(parts[2]));
                        } else {
                            continue;
                        }


                        if (isDateInRange(normalizedDate, startDate, endDate)) {
                            orderList.add(order);
                        }
                    }
                }

                Collections.sort(orderList, (o1, o2) ->
                        o1.getPartner().compareToIgnoreCase(o2.getPartner())
                );
                adapter.notifyDataSetChanged();
                updateSummary(orderList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrdersActivity.this, "Hiba a lekérdezéskor", Toast.LENGTH_SHORT).show();
            }
        });
    }





    public void updateSummaryFromAdapter() {
        if (adapter != null) {
            List<OrderProduct> currentOrders = adapter.getOrderList();
            updateSummary(currentOrders);
        }
    }
    private void updateSummary(List<OrderProduct> orders) {
        TextView summaryTV = findViewById(R.id.SummaryTextView);


        Map<String, Integer> totals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (OrderProduct order : orders) {
            String product = order.getProduct();
            int quantity = order.getQuantity();
            totals.put(product, totals.getOrDefault(product, 0) + quantity);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            sb.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" db\n");
        }

        summaryTV.setText(sb.toString());
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


    private void sortByPartner() {
        Collections.sort(orderList, (o1, o2) -> {
            int cmp = o1.getPartner().compareToIgnoreCase(o2.getPartner());
            return partnerSortAsc ? cmp : -cmp;
        });
        partnerSortAsc = !partnerSortAsc;
        adapter.notifyDataSetChanged();
    }

    private void sortByProduct() {
        Collections.sort(orderList, (o1, o2) -> {
            int cmp = o1.getProduct().compareToIgnoreCase(o2.getProduct());
            return productSortAsc ? cmp : -cmp;
        });
        productSortAsc = !productSortAsc;
        adapter.notifyDataSetChanged();
    }

    private void sortByDate() {
        Collections.sort(orderList, (o1, o2) -> {
            int cmp = o1.getDate().compareToIgnoreCase(o2.getDate());
            return dateSortAsc ? cmp : -cmp;
        });
        dateSortAsc = !dateSortAsc;
        adapter.notifyDataSetChanged();
    }

    private void sortByQuantity() {
        Collections.sort(orderList, (o1, o2) -> {
            int cmp = Integer.compare(o1.getQuantity(), o2.getQuantity());
            return quantitySortAsc ? cmp : -cmp;
        });
        quantitySortAsc = !quantitySortAsc;
        adapter.notifyDataSetChanged();
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) productsRef.removeEventListener(productsListener);
        if (ordersListener != null) ordersRef.removeEventListener(ordersListener);

    }
    private String formatDateKey(Calendar cal) {
        return String.format(Locale.getDefault(), "%04d.%02d.%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}