package com.yasingok.newtravelbook.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.yasingok.newtravelbook.R;
import com.yasingok.newtravelbook.databinding.ActivityMapsBinding;
import com.yasingok.newtravelbook.roomdb.PlaceDao;
import com.yasingok.newtravelbook.roomdb.PlaceDatabase;

import java.util.Arrays;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    SharedPreferences sharedPreferences;
    boolean info;
    PlaceDatabase db;
    PlaceDao placeDao;
    Double selectedLong;
    Double selectedLat;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    com.yasingok.newtravelbook.model.Place selectedPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();

        binding.saveButton.setVisibility(View.VISIBLE);
        binding.deleteButton.setVisibility(View.INVISIBLE);

        try{
            Places.initialize(getApplicationContext(), "KEY");

            PlacesClient placesClient = Places.createClient(this);

            AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autoComplete);
            autocompleteSupportFragment.setTypesFilter(Arrays.asList(PlaceTypes.RESTAURANT));
            autocompleteSupportFragment.setCountries("TR");
            //autocompleteSupportFragment.setLocationRestriction(RectangularBounds.newInstance(
                    //new LatLng(35.808592, 26.041319),
                    //new LatLng(42.110291, 44.793550)));
            autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

            autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onError(@NonNull Status status) {
                    System.out.println("Hata: " + status);
                }

                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    System.out.println(place.getName());
                    LatLng yeni = place.getLatLng();
                    if (yeni != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yeni, 15));
                    } else {
                        // Hata durumuyla ilgili bir işlem yapabilirsiniz.
                        Log.e("MapsActivity", "LatLng is null");
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }

        // İlk başta sadece 1 kere konum değişikliğini sorgulayıp alıp sonrasında başa dönmemesi için
        sharedPreferences = MapsActivity.this.getSharedPreferences("com.yasingok.travelbook", MODE_PRIVATE);
        info = false;

        // Veritabanını kuruyoruz
        db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class, "Places")
                //.allowMainThreadQueries()           // Bunu yaparsak kullanıcıyı negatif ekleyebilir performans açısından
                .build();
        placeDao = db.placeDao();
        selectedLong = 0.0;     // Varsayılan değerler olarak sıfırladık
        selectedLat = 0.0;

        binding.saveButton.setEnabled(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(MapsActivity.this);   //listenerı kurduk

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");

        if(intentInfo.equals("new")){
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            // Gelen nesnenin locationmanager sınıfından olduğunu gösterip servisi ayarlıyoruz
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override       // Konum değişince ne olacak kısmı
                public void onLocationChanged(@NonNull Location location) {
                    info = sharedPreferences.getBoolean("info", false);
                    if (!info){
                        double yeniLong = location.getLongitude();
                        double yeniLat = location.getLatitude();
                        LatLng konum = new LatLng(yeniLat, yeniLong);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(konum,15));
                        sharedPreferences.edit().putBoolean("info", true).apply();
                    }
                }
            };

            // İzin kabul edilmemişse
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED){

                // Kullanıcıya izin hakkında bilgi vermek
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(), "You need to accept location info permission",
                            Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {    // Burada da izin alınacak
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                } else{     // Burada da izin istenecek
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }else{  // İzin verilmişse
                // Bunu kullanmak için öncelikle izin almalıyız
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
                        locationListener);
                // Son bilinen konum varsa oraya odaklanır başta
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null){
                    LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 15));
                }
                // Konumumuz mavi küçük bir nokta ile görünür
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);      // Zoom yapabilmek için buton

                //mMap.getUiSettings().setMapToolbarEnabled(true);
                //mMap.getUiSettings().setCompassEnabled(true);
                //mMap.getUiSettings().setAllGesturesEnabled(true);
                //mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
                //mMap.getUiSettings().setMyLocationButtonEnabled(true);

                //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);       // Uydu ve normal beraber
                //mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);       // Normal
                //mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);      // Yükseklikler de var
            }
            // latitude: enlem,  longitude: boylam
        }else{
            mMap.clear();
            selectedPlace = (com.yasingok.newtravelbook.model.Place) intent.getSerializableExtra("place");
            LatLng latLng = new LatLng(selectedPlace.latitude, selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            binding.nameText.setText(selectedPlace.name);
            binding.infoText.setText(selectedPlace.description);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean o) {
                        if (o){     // İzin verildi
                            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED){
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1,
                                        locationListener);

                                // Son bilinen konum varsa oraya odaklanır başta
                                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (lastLocation != null){
                                    LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 15));
                                }
                            }
                        }else{      // İzin reddedildi
                            Toast.makeText(MapsActivity.this, "You need to give location permisson!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {    // Uzun tıkladığımızda olacak olay için
        mMap.clear();       // Öncekileri temizler
        mMap.addMarker(new MarkerOptions().position(latLng).title("Choosen location"));
        selectedLat = latLng.latitude;
        selectedLong = latLng.longitude;
        binding.saveButton.setEnabled(true);
    }

    public void back(View view){
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void save(View view){
        com.yasingok.newtravelbook.model.Place place = new com.yasingok.newtravelbook.model.Place(binding.nameText.getText().toString(), binding.infoText.getText().toString(), selectedLong, selectedLat);
        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())                   // Arka planda çalışması için
                .observeOn(AndroidSchedulers.mainThread())      // Mainde gözlemleyebilmek için
                .subscribe(MapsActivity.this::handleResponse)   // Referans verdik
        );
    }

    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void delete(View view){
        if (selectedPlace != null){
            compositeDisposable.delete(placeDao.delete(selectedPlace)
                    .subscribeOn(Schedulers.io())                   // Arka planda çalışması için
                    .observeOn(AndroidSchedulers.mainThread())      // Mainde gözlemleyebilmek için
                    .subscribe(MapsActivity.this::handleResponse)   // Referans verdik
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();    // Kapandıktan sonra fonksiyon çağırmalarını kapatıp hafızayı verimli yapıyor
    }
}
