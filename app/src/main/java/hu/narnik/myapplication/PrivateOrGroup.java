package hu.narnik.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class PrivateOrGroup extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


      //  EdgeToEdge.enable(this);
        setContentView(R.layout.activity_private_or_group);

        Button privateButton = findViewById(R.id.privateButton);
        privateButton.setOnClickListener(v -> {

            AppState.currentNoteType = "PRIVÁT";
            AppState.isGroupView = false;


            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                    .putString("lastNoteType", "PRIVÁT")
                    .apply();


            Intent intent = new Intent(PrivateOrGroup.this, BaseActivity.class);
            intent.putExtra("NOTE_TYPE", "PRIVÁT");
            startActivity(intent);
            finish();
        });

        Button groupButton = findViewById(R.id.groupButton);
        groupButton.setOnClickListener(v -> {
            AppState.currentNoteType = "CSOPORT";
            AppState.isGroupView = true;


            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                    .putString("lastNoteType", "CSOPORT")
                    .apply();

            String lastGroupId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .getString("lastGroupId", null);

            if (lastGroupId != null) {

                AppState.currentGroupId = lastGroupId;
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                        .putString("lastGroupId", lastGroupId)
                        .apply();

                Intent intent = new Intent(PrivateOrGroup.this, BaseActivity.class);
                intent.putExtra("NOTE_TYPE", "CSOPORT");
                startActivity(intent);
            } else {

                Intent intent = new Intent(PrivateOrGroup.this, GroupSelectionActivity.class);
                startActivity(intent);
            }

            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {

                    finish();
                    return;
                }


                doubleBackToExitPressedOnce = true;
                Toast.makeText(PrivateOrGroup.this, "Nyomja meg még egyszer a kilépéshez", Toast.LENGTH_SHORT).show();


                new android.os.Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
            }
        });


    }
}