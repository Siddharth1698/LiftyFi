package com.siddharthm.liftyfi;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {
    private EditText mEmail,mPassword;
    private Button mLogin,mRegister;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;
    private FirebaseUser user;
    private String email,password,uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);
        auth = FirebaseAuth.getInstance();

        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = FirebaseAuth.getInstance().getCurrentUser();
                      if (user!=null){
                          Intent intent = new Intent(DriverLoginActivity.this,DriverMapsActivity.class);
                          startActivity(intent);
                          finish();
                          return;
                      }
            }
        };
        mEmail = (EditText)findViewById(R.id.emailDriver);
        mPassword = (EditText) findViewById(R.id.passwordDriver);
        mLogin = (Button)findViewById(R.id.loginDriverBtn);
        mRegister = (Button)findViewById(R.id.registerDriverBtn);





        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();
                auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                           if (task.isSuccessful()){
                               uid = auth.getUid();
                               DatabaseReference current_user_db = FirebaseDatabase.getInstance()
                                       .getReference().child("Users").child("Drivers").child(uid).child("name");
                               current_user_db.setValue(email);
                               Toast.makeText(DriverLoginActivity.this,"Succesfully Registered",Toast.LENGTH_SHORT).show();
                           }else {
                               Toast.makeText(DriverLoginActivity.this,"Error signing in",Toast.LENGTH_SHORT).show();
                           }
                    }
                });

            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();
                auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(DriverLoginActivity.this,"Succesfully Signed in",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(DriverLoginActivity.this,"Error signing in",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


    }
    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(firebaseAuthListner);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(firebaseAuthListner);
    }

}
