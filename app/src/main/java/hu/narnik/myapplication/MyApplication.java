package hu.narnik.myapplication;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        FirebaseApp.initializeApp(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app"
        );

        // Offline mentés engedélyezése
        database.setPersistenceEnabled(true);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder().build()
                )
                .build();
        db.setFirestoreSettings(settings);
    }
}