package com.siddharthm.liftyfi;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener {

    GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button logout,request,settings;
    private LatLng pickuplocation;
    private String customerId = "",requestService;
    private Boolean requestbol = false;
    private Marker pickupMarker;
    private String destination;
    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName,mDriverPhone,mDriverCar;
    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        logout = (Button)findViewById(R.id.logoutdriver);
        request = (Button)findViewById(R.id.callLift);
        settings = (Button)findViewById(R.id.settings);
        mDriverInfo = (LinearLayout)findViewById(R.id.driverInfo);
        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mRadioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.liftyx);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView)findViewById(R.id.driverCar);
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestbol){
                    requestbol = false;
                    driverLocationRef.removeEventListener(driverLocationRefListner);
                    geoQuery.removeAllListeners();
                    if (driverFoundId != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                        driverRef.removeValue();
                        driverFoundId = null;
                    }
                    driverFound = false;
                    radius = 1;
                    String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userid);
                    if (pickupMarker != null){
                        pickupMarker.remove();
                    }
                    request.setText("Liftyfi Now");

                    mDriverInfo.setVisibility(View.GONE);
                    mDriverName.setText("");
                    mDriverCar.setText("");
                    mDriverCar.setText("" );
                }else{

                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton)findViewById(selectId);
                    if (radioButton == null){
                        return;
                    }

                    requestService = radioButton.getText().toString();

                    requestbol = true;
                    String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userid,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                    pickuplocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickuplocation).title("I AM HERE").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pickup)));
                    request.setText("Getting your driver...");

                    getClosestDriver();
                }


            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                destination = place.getName().toString();
            }

            @Override
            public void onError(Status status) {

            }
        });

    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;
    private DatabaseReference driverLocation;

    private void getClosestDriver() {
       driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickuplocation.latitude,pickuplocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestbol){
                    DatabaseReference mCustomerDtabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDtabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                                Map<String,Object> drivermap = (Map<String, Object>) dataSnapshot.getValue();

                                if (driverFound){
                                    return;
                                }
                                if (drivermap.get("service").equals(requestService)){
                                    driverFound = true;
                                    driverFoundId = dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");

                                    HashMap map = new HashMap();
                                    map.put("customerRideId",customerId);
                                    map.put("destination",destination);
                                    driverRef.updateChildren(map);


                                    request.setText("Looking for Driver Location...");
                                    getDriverLocation();
                                    getDriverInfo();

                                }

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name")!=null){

                        mDriverName.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!=null){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("car")!=null){
                        mDriverCar.setText(map.get("car").toString());
                    }
                    if (map.get("profileImageUrl")!=null){
                        Picasso.get().load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private Marker driverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListner;

    public void getDriverLocation() {

        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListner = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestbol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    request.setText("Driver Found");
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);

                    if (driverMarker != null){
                        driverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickuplocation.latitude);
                    loc1.setLongitude(pickuplocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100){
                        request.setText("Driver's Here");
                    }else{
                        request.setText("Driver Found: " + String.valueOf(distance));
                    }
                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));



    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}