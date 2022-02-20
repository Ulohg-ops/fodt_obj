package com.example.foodtopia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.foodtopia.restaurant.RestaurantFragment;
import com.example.foodtopia.social.SocialFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    Fragment fragment = new Dashboard();


//    check whether user log in  if not intent to login
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if(FirebaseAuth.getInstance().getCurrentUser()==null){
//            startActivity(new Intent(this,Login.class));
//            finish();
//        }
//    }

    //    private Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        //顯示首頁
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();




        //點選下方工具列切換頁面
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.Dashboard:
                        fragment = new Dashboard();
                        break;
                    case R.id.Restaurant:
                        fragment = new RestaurantFragment();
                        break;
                    case R.id.Add:
                        fragment = new Add();
                        break;
                    case R.id.Social:
                        fragment = new SocialFragment();
                        break;
                    case R.id.Account:
                        fragment = new Account();
                        break;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
                return true;
            }
        });

//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("message");
//
//        myRef.setValue("Hello world!");
    }
}