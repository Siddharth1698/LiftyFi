package com.siddharthm.liftyfi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

public class CustomerSettingsActivity extends AppCompatActivity {
    private EditText mName,mPhone;
    private Button mConfirm,mBack;
    private FirebaseAuth auth;
    private DatabaseReference mCustomerDatabase;
    private String userId;
    private String name,phone;
    private ImageView mProfileImage;
    private Uri resultUri;
    private String profileImageUrl;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        mName = (EditText)findViewById(R.id.name);
        mPhone = (EditText)findViewById(R.id.phone);
        mConfirm = (Button)findViewById(R.id.confirm);
        mBack = (Button)findViewById(R.id.back);
        mProfileImage = (ImageView)findViewById(R.id.profileimage);
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        getUserInfo();
        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });
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
                    if (map.get("profileImageUrl")!=null){
                        profileImageUrl = map.get("profileImageUrl").toString();
                        Picasso.get().load(profileImageUrl).into(mProfileImage);
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
        if (resultUri != null) {

            StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filepath.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(CustomerSettingsActivity.this,"Some Error Occured",Toast.LENGTH_SHORT).show();
                  finish();
                  return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl",downloadUri.toString());
                    mCustomerDatabase.updateChildren(newImage);
                    finish();
                    return;
                }
            });

        }else {
            finish();
        }



    }
}
