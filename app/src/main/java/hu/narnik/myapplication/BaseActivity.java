package hu.narnik.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;
    protected String noteType;

    private String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

    private boolean doubleBackToExitPressedOnce = false;

    protected boolean isBaseActivityInstance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        isBaseActivityInstance = getClass().equals(BaseActivity.class);

        setContentView(R.layout.activity_base);

        noteType = getIntent().getStringExtra("NOTE_TYPE");
        if (noteType != null) {
            AppState.currentNoteType = noteType;
        }

        if ("CSOPORT".equals(AppState.currentNoteType) && AppState.currentGroupId == null) {
            String savedGroupId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .getString("lastGroupId", null);
            if (savedGroupId != null) {
                AppState.currentGroupId = savedGroupId;
                Log.d("BaseActivity", "Visszatöltött csoport ID: " + savedGroupId);
            }
        }


        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        View headerView = navigationView.getHeaderView(0);
        TextView headerTitle = headerView.findViewById(R.id.headerTitle);

        if ("CSOPORT".equalsIgnoreCase(AppState.currentNoteType)) {
            headerTitle.setText("Csoport nézet (" + AppState.currentGroupId + ")");
        } else {
            headerTitle.setText("Privát nézet");
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        navigationView.getMenu().findItem(R.id.nav_newMember).setVisible(false);


        if ("CSOPORT".equals(AppState.currentNoteType) && AppState.currentGroupId != null) {
            String currentGroupId = AppState.currentGroupId;
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


            android.util.Log.d("BaseActivity", "Csoport ID: " + currentGroupId);
            android.util.Log.d("BaseActivity", "Bejelentkezett felhasználó UID: " + uid);

            DatabaseReference adminRef = FirebaseDatabase.getInstance(
                    "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app"
            ).getReference("groups").child(currentGroupId).child("admin").child("uid");

            adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String adminId = snapshot.getValue(String.class);
                    android.util.Log.d("BaseActivity", "Admin ID az adatbázisból: " + adminId);

                    boolean isAdmin = uid.equals(adminId);

                    android.util.Log.d("BaseActivity", "Admin státusz: " + isAdmin);

                    runOnUiThread(() -> {
                        navigationView.getMenu().findItem(R.id.nav_newMember).setVisible(isAdmin);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.e("BaseActivity", "Firebase adatbázis hiba: " + error.getMessage());
                }
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }

                if (isBaseActivityInstance) {
                    // csak BaseActivity-ben dupla back kilépés
                    if (doubleBackToExitPressedOnce) {
                        finishAffinity();
                        return;
                    }

                    doubleBackToExitPressedOnce = true;
                    Toast.makeText(BaseActivity.this, "Nyomja meg még egyszer a kilépéshez", Toast.LENGTH_SHORT).show();

                    new android.os.Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                } else {



                    finish();
                }
            }
        });




    }
    private void updateViewModeText() {
        TextView viewModeText = findViewById(R.id.viewModeText);
        if (viewModeText != null) {
            if ("CSOPORT".equalsIgnoreCase(AppState.currentNoteType)) {
                viewModeText.setText("Csoport nézet (" + AppState.currentGroupId + ")");
            } else {
                viewModeText.setText("Privát nézet");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Ha nincs Intentből jövő NOTE_TYPE -> SharedPreferences
        if (getIntent().getStringExtra("NOTE_TYPE") == null) {
            AppState.currentNoteType = prefs.getString("lastNoteType", "PRIVAT");
            if ("CSOPORT".equals(AppState.currentNoteType)) {
                AppState.currentGroupId = prefs.getString("lastGroupId", null);
            }
        }

        updateNavigationHeader();
        updateViewModeText();

        if ("CSOPORT".equals(AppState.currentNoteType) && AppState.currentGroupId != null) {
            checkAdminStatus();
        }
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView headerTitle = headerView.findViewById(R.id.headerTitle);
        if ("CSOPORT".equalsIgnoreCase(AppState.currentNoteType)) {
            headerTitle.setText("Csoport nézet (" + AppState.currentGroupId + ")");
        } else {
            headerTitle.setText("Privát nézet");
        }
    }

    private void checkAdminStatus() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("groups")
                .child(AppState.currentGroupId)
                .child("admin")
                .child("uid");

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String adminId = snapshot.getValue(String.class);
                boolean isAdmin = uid.equals(adminId);
                runOnUiThread(() -> navigationView.getMenu().findItem(R.id.nav_newMember).setVisible(isAdmin));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    protected void setContent(@LayoutRes int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        LayoutInflater.from(this).inflate(layoutResID, contentFrame, true);
    }


    public void setToolbarTitle(String noteType, String sectionName) {
        if (toolbar != null) {
            String title = noteType != null ? noteType : "";
            if (sectionName != null) {
                title += title.isEmpty() ? sectionName : " - " + sectionName;
            }
            toolbar.setTitle(title);
        }
    }


    @Override
    public boolean onNavigationItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_notes) {
            startActivity(new Intent(this, NotesActivity.class));
        } else if (id == R.id.nav_contacts) {
            startActivity(new Intent(this, ContactsActivity.class));
        } else if (id == R.id.nav_signout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_switch) {
            startActivity(new Intent(this, PrivateOrGroup.class));
            finish();
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, OrdersActivity.class));

        } else if (id == R.id.nav_manageData) {
            startActivity(new Intent( this, ManageDataActivity.class));

        } else if (id == R.id.nav_newMember) {
            startActivity(new Intent(this, NewMember.class));

        } else if (id == R.id.nav_groupSelection) {
            Intent intent = new Intent(this, GroupSelectionActivity.class);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;


    }
}