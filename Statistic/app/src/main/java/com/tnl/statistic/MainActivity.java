package com.tnl.statistic;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tnl.statistic.ui.dashboard.ImportFragment;
import com.tnl.statistic.ui.home.HomeFragment;
import com.tnl.statistic.ui.home.HomeViewModel;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private Map<String, ImportFragment.Data> dataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Restore data map if needed
        if (savedInstanceState != null) {
            dataMap = (Map<String, ImportFragment.Data>) savedInstanceState.getSerializable("dataMap");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    // Method to update data and pass it to the HomeFragment
    public void updateData(Map<String, ImportFragment.Data> newDataMap) {
        this.dataMap = newDataMap;

        // Use ViewModel to update HomeFragment data
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.setDataMap(newDataMap);

        // Navigate to HomeFragment
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_home);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the data map to restore it later
        outState.putSerializable("dataMap", new HashMap<>(dataMap));
    }
}
