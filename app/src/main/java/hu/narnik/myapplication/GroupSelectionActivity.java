package hu.narnik.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class GroupSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);




        setContentView(R.layout.activity_group_selection);


        Button joinButton = findViewById(R.id.joinButton);
        joinButton.setOnClickListener(v -> {
            Intent intent = new Intent(GroupSelectionActivity.this, JoinGroup.class);
            startActivity(intent);
            finish();
        });

        Button newGroupButton = findViewById(R.id.newGroupButton);
        newGroupButton.setOnClickListener(v -> {
            Intent intent = new Intent(GroupSelectionActivity.this, NewGroup.class);
            startActivity(intent);
            finish();
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Intent intent = new Intent(GroupSelectionActivity.this, PrivateOrGroup.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });


    }
}