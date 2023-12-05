package com.yasingok.newtravelbook.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.yasingok.newtravelbook.R;
import com.yasingok.newtravelbook.adapter.PlaceAdapter;
import com.yasingok.newtravelbook.databinding.ActivityMainBinding;
import com.yasingok.newtravelbook.model.Place;
import com.yasingok.newtravelbook.roomdb.PlaceDao;
import com.yasingok.newtravelbook.roomdb.PlaceDatabase;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mainBinding;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    PlaceDatabase db;
    PlaceDao placeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mainBinding.getRoot();
        setContentView(view);

        // Veritabanını kuruyoruz
        db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class, "Places").build();
        placeDao = db.placeDao();

        compositeDisposable.add(placeDao.getAll()
                .subscribeOn(Schedulers.io())               // Arka planda çalışması için
                .observeOn(AndroidSchedulers.mainThread())  // Mainde gözlemleyebilmek için
                .subscribe(MainActivity.this::handleResponse)
        );
    }

    private void handleResponse(List<Place> placeList){     // Burada flowable halinden listeyi çekmek için yapıyoruz
        mainBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PlaceAdapter placeAdapter = new PlaceAdapter(placeList);
        mainBinding.recyclerView.setAdapter(placeAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.travel_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuId){
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("info", "new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}