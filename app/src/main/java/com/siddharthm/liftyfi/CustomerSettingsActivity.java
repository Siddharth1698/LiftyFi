package com.siddharthm.liftyfi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {
    private EditText mName,mPhone;
    private Button mConfirm,mBack;
    private FirebaseAuth auth;
    private DatabaseReference mCustomerDatabase;
    private String userId;
    private String name,phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        mName = (EditText)findViewById(R.id.name);
        mPhone = (EditText)findViewById(R.id.phone);
        mConfirm = (Button)findViewById(R.id.confirm);
        mBack = (Button)findViewById(R.id.back);
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        getUserInfo();
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });

    }
    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name")!=null){
                        name = map.get("name").toString();
                        mName.setText(name);
                    }
                    if (map.get("phone")!=null){
                        phone = map.get("phone").toString();
                        mPhone.setText(phone);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {
        name = mName.getText().toString();
        phone = mPhone.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name",name);
        userInfo.put("phone",phone);
        mCustomerDatabase.updateChildren(userInfo);
        finish();


    }
}
