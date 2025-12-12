package hu.narnik.myapplication;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executors;


public class GoogleSignInActivity extends AppCompatActivity {

    private static final String TAG = "GoogleActivity";


    private FirebaseAuth mAuth;

    private CredentialManager credentialManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_google_sign_in);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());


        SignInButton sign_in_button = findViewById(R.id.sign_in_button);
        sign_in_button.setOnClickListener(v -> {

            launchCredentialManager();
        });
        Button emailLogInButton = findViewById(R.id.emailLogInButton);
        emailLogInButton.setOnClickListener(v -> {
            Intent intent = new Intent(GoogleSignInActivity.this, EmailLogInActivity.class);
            startActivity(intent);
        });

        Button regButton = findViewById(R.id.regButton);
        regButton.setOnClickListener(v -> {
            Intent intent = new Intent(GoogleSignInActivity.this, emailRegister.class);
            startActivity(intent);
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Intent intent = new Intent(GoogleSignInActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    // [END on_start_check_user]

    private void launchCredentialManager() {
        // [START create_credential_manager_request]
        // Instantiate a Google sign-in request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
        // [END create_credential_manager_request]

        // Launch Credential Manager UI
        credentialManager.getCredentialAsync(
                GoogleSignInActivity.this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Extract credential from the result returned by Credential Manager
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Couldn't retrieve user's credentials: " + e.getLocalizedMessage());
                        runOnUiThread(() ->
                                Toast.makeText(GoogleSignInActivity.this,
                                        "Hiba a Credential Manager-ben: " + e.getLocalizedMessage(),
                                        Toast.LENGTH_LONG).show());
                    }
                }
        );
    }

    // [START handle_sign_in]
    private void handleSignIn(Credential credential) {
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            // Create Google ID Token
            Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            Log.w(TAG, "Credential is not of type Google ID!");
        }
    }
    // [END handle_sign_in]

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");

                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToDatabase(user);
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        runOnUiThread(() ->
                                Toast.makeText(GoogleSignInActivity.this,
                                        "Firebase hitelesítés sikertelen: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                        );
                        updateUI(null);
                    }
                });
    }
    // [END auth_with_google]


    private void saveUserToDatabase(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String email = user.getEmail();
        String databaseUrl = "https://summa-2ba25-default-rtdb.europe-west1.firebasedatabase.app";

        DatabaseReference usersRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users");


        usersRef.child(uid).child("email").setValue(email)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UsersDB", "User saved: " + email);
                })
                .addOnFailureListener(e -> {
                    Log.e("UsersDB", "Hiba a felhasználó mentésekor: " + e.getMessage());
                });
    }



    // [START sign_out]
    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // When a user signs out, clear the current user credential state from all credential providers.
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        updateUI(null);
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "Couldn't clear user credentials: " + e.getLocalizedMessage());
                    }
                });
    }
    // [END sign_out]

    private void updateUI(FirebaseUser user) {
        if (user != null) {

            Intent intent = new Intent(GoogleSignInActivity.this, PrivateOrGroup.class);
            startActivity(intent);
            finish();
        }
    }


}