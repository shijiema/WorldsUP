package io.worldsup.worldsup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import io.worldsup.worldsup.state.MapStateManager;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private MapStateManager stateMgr = null;
    private final int ZOOM_DEFAULT=15;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private Location mCurrentLocation;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private GoogleMap mMap;
    //for google service access
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted;
    private LatLng defaultLocationStanleyPark = new LatLng(49.299598,-123.144162);

    private void setupMapIfNeeded() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }
    /**
     * This method is triggered by getMapAsync
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
        Log.d("ama","in onMapReady");
        mMap = googleMap;
        if(stateMgr.getCameraPosition()==null) {
            checkDeviceLocation();
        }else{
            restoreMapState();
        }
        setMarkerClickAdapter(mMap);
        setInfoWindowClickAdapter(mMap);
        setLongClickAdapter(mMap);
        setInfoWindowAdaptor(mMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ama","in onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        setupMapIfNeeded();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        stateMgr = new MapStateManager(this);
        EditText etLocation = (EditText) findViewById(R.id.editLocation);
        etLocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    gotoUserNamedPlace(v);
                    handled=true;
                }
                return handled;
            }
        });
    }

    private void setInfoWindowAdaptor(GoogleMap map){
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View info_window = getLayoutInflater().inflate(R.layout.info_window,null);
                TextView infoSummary = info_window.findViewById(R.id.textSummary);
                infoSummary.setText("information summary");
                ImageView iv = info_window.findViewById(R.id.imageView);
                iv.setImageResource(R.drawable.ic_anwser);
                marker.setAnchor(0.5f,0.5f);
                return info_window;
            }
        });
    }
    /**
     * long clicking will bring up question asking windows.
     * I will make a draggable marker for user to drop to a place/cell to ask question too
     * @param map
     */
    private void setLongClickAdapter(GoogleMap map){
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                MarkerOptions optns = createQuestionMarker(latLng,"Location Related Questions");
                mMap.addMarker(optns);
                //then display question capture window
            }
        });
    }
    private void setMarkerClickAdapter(GoogleMap map){
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //if(marker.isInfoWindowShown()){marker.hideInfoWindow();return true;}

                Toast.makeText(MapsActivity.this, "Clicked on: " + marker.toString()+". this also brings infor window", Toast.LENGTH_SHORT).show();
                return false;//true will stop propagating the event and information will not be displayed
            }
        });
    }
    private void setInfoWindowClickAdapter(GoogleMap map){
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Toast.makeText(MapsActivity.this, "Info windows Clicked: this will bring up detail window.", Toast.LENGTH_SHORT).show();
                marker.hideInfoWindow();
            }
        });
    }

    /**
     * Gets the current location of the device and starts the location update notifications.
     */
    @SuppressWarnings("MissingPermission")
    private void checkDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        //check if it has the permission
        if (mLocationPermissionGranted  || (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mLocationPermissionGranted = true;
            updateDefaultMapUIFeatures();

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mCurrentLocation = location;
                                gotoCurrentLocation();
                            }
                        }
                    });
//            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
//                @Override
//                public void onComplete(@NonNull Task<Location> task) {
//                    if (task.isSuccessful() && task.getResult() != null) {
//                        mCurrentLocation = task.getResult();
//                    }
//                }
//            });;
        } else {
            Toast.makeText(this, "location permission is not granted! try to ask one.", Toast.LENGTH_LONG).show();
            //explicitly request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }
    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        if(mLocationPermissionGranted) {
            updateDefaultMapUIFeatures();
            checkDeviceLocation();
        }
    }
    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    @SuppressWarnings("MissingPermission")
    private void updateDefaultMapUIFeatures() {
        if (mMap == null) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void gotoCurrentLocation() {
        LatLng curGps = null;
        MarkerOptions marker = null;
        CameraUpdate update = null;
        if (mCurrentLocation == null) {
                Toast.makeText(this, "No location info, use default location of Vancouver BC Canada!", Toast.LENGTH_SHORT).show();
                curGps = defaultLocationStanleyPark;
                update = CameraUpdateFactory.newLatLngZoom(
                        curGps, 15
                );
//                marker =createQuestionMarker(curGps,"Stanley Park");
//                marker.snippet("BC, Canada");
        } else {

                curGps = new LatLng(
                        mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude()
                );
//                marker =createQuestionMarker(curGps,"Touch");
//                marker.snippet("to ask location related questions");
                update = CameraUpdateFactory.newLatLngZoom(
                        curGps, 15
                );
                //mMap.animateCamera(update);
        }
//            mMap.addMarker(marker);
//            mMap.moveCamera(update);

    }
    private void hideSoftKeyboard(View v){
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(),0);
    }

    /**
     * center the map to a location
     * @param lat
     * @param lng
     * @param zoom
     */
    private void gotoLocation(double lat, double lng,float zoom){
        Objects.requireNonNull(lat);
        Objects.requireNonNull(lng);
        LatLng latLng = new LatLng(lat,lng);
        CameraUpdate cameraUpdate =  CameraUpdateFactory.newLatLngZoom(latLng,zoom);
        mMap.moveCamera(cameraUpdate);
    }
    public void gotoUserNamedPlace(View view)  {
        hideSoftKeyboard(view);

        TextView tv = (TextView) findViewById(R.id.editLocation);
        String searchString = tv.getText().toString();
        if(searchString==null ||"".equals(searchString.trim()))return;

        //use geocoder to parse out address or name to gps coordinations
        Geocoder gcder = new Geocoder(this);
        try {
            List<Address> listOfAddress = gcder.getFromLocationName(searchString,1);
            if(listOfAddress!=null && listOfAddress.size()>0){
                Address addr = listOfAddress.get(0);
                String locality = addr.getLocality();
                //Toast.makeText(this, "Found: " + locality, Toast.LENGTH_SHORT).show();
                gotoLocation(addr.getLatitude(),addr.getLongitude(),ZOOM_DEFAULT);
                final Marker marker = mMap.addMarker(createInfoMarker(new LatLng(addr.getLatitude(), addr.getLongitude()), locality==null?addr.getAddressLine(0):locality));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private MarkerOptions createInfoMarker(LatLng latLng,String title){
        Objects.requireNonNull(latLng);
        MarkerOptions marker = new MarkerOptions().title(title).position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        marker.snippet("300 piece of information. Click to view");
        return marker;
    }
    private MarkerOptions createQuestionMarker(LatLng latLng,String title){
        Objects.requireNonNull(latLng);
        MarkerOptions marker = new MarkerOptions().title(title).position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        marker.snippet("100 questions waiting for answer. Click to answer.");
        return marker;
    }
//do this using onSaveInstanceState,(onCreate or onRestoreInstanceState)

    @Override
    protected void onPause() {
        Log.d("ama","in onPause");
        stateMgr.saveMapState(mMap);
        super.onPause();

    }

    @Override
    protected void onResume() {
        Log.d("ama","in onResume");
        super.onResume();
        //restoreMapState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ama","in onDestroy");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ama","in onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ama","in onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("ama","in onRestart");
    }

    private void restoreMapState(){
        Log.d("ama","in restoreMapState");
        CameraPosition cameraPosition = stateMgr.getCameraPosition();
        if(cameraPosition!=null){
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            if(mMap!=null) {
                mMap.moveCamera(cameraUpdate);
            }
        }
    }
    /**
     * zoom the map to view detail question or information
     * group questions and informations into 2 different markers
     * split screen into 16 areas, detail information is viewiable when number of information is less than 100. if at biggest zoom level already, scroll the information
     * within each areas, display two markers for questions and information.
     * question can be selected to answer
     * information can be liked or disliked trust or distrust. being distrusted by 5 person will remove the information.
     * question or information lives only for one day
     */
}
