package com.barmpas.asthma;


import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.barmpas.asthma.data.SessionContract;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.view.View.GONE;

/*
 * The Login activity allows the user to connect to firebase if he/she is registered or not.
 * Upon correct verification it sends the SQlite database to the Online Firebase using cursor to traverse it.
 */


public class LoginActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_SIGNUP = 0;
    private static final int URL_LOADER = 0;

    EditText _emailText;
    EditText _passwordText;
    Button _loginButton;
    TextView _signupLink;
    LinearLayout linearLayout, sending;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    DatabaseReference mDatabase;

    boolean signed;

    Cursor cursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        _emailText = findViewById(R.id.input_email);
        _passwordText = findViewById(R.id.input_password);
        _loginButton = findViewById(R.id.btn_login);
        _signupLink = findViewById(R.id.link_signup);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        linearLayout = findViewById(R.id.share_layout);
        sending = findViewById(R.id.sending);
        sending.setVisibility(GONE);
        signed = false;
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });

        getLoaderManager().initLoader(URL_LOADER, null, this);
    }

    public void login() {

        String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();


        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Please enter board number", Toast.LENGTH_SHORT).show();
            return;
        }

        _emailText.setText("");
        _passwordText.setText("");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success call send() function. The handler is implemented for UI purposes.
                            signed = true;
                            sending.setVisibility(View.VISIBLE);
                            linearLayout.setVisibility(GONE);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    send();
                                }
                            }, 3000);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    //Initializing the cursor.

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        switch (loaderID) {
            case URL_LOADER:
                String[] projection = {
                        SessionContract.SessionEntry._ID,
                        SessionContract.SessionEntry.COLUMN_ID,
                        SessionContract.SessionEntry.COLUMN_BREATHES_IN,
                        SessionContract.SessionEntry.COLUMN_ERROR
                };
                return new CursorLoader(
                        this,
                        SessionContract.SessionEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    //Send the data using the cursor

    public void send() {
        if (signed) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            DatabaseReference users = mDatabase.child("users");
            final DatabaseReference ref = users.child(currentUser.getUid());
            if (cursor.moveToFirst()) {
                do {
                    final Long date = cursor.getLong(cursor.getColumnIndex("id"));
                    final String breathes = cursor.getString(cursor.getColumnIndex("bin"));
                    final String error = cursor.getString(cursor.getColumnIndex("ain"));
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (!snapshot.hasChild(String.valueOf(date))) {
                                DatabaseReference ref_breathes = ref.child(String.valueOf(date)).child("breathes");
                                ref_breathes.setValue(breathes);
                                DatabaseReference ref_air = ref.child(String.valueOf(date)).child("air");
                                ref_air.setValue(error);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                } while (cursor.moveToNext());
            }
            cursor.close();
            sending.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            Toast.makeText(LoginActivity.this, "Successfully Uploaded Data.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor mCursor) {
        cursor = mCursor;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }
}