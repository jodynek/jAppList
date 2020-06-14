package com.jodynek.japplist;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements OnQueryTextListener {
  private SearchView searchView;
  private MenuItem searchMenuItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // permission for stats
    AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
    if (appOps == null)
      return;
    int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(), getPackageName());
    if (mode != AppOpsManager.MODE_ALLOWED) {
      startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 0);
    }
    // permission for writing into external storage
    /* Request user permissions in runtime */
    ActivityCompat.requestPermissions(MainActivity.this,
        new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        },
        100);

    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (fragment == null)
          return;
        AppsListFragment appsListFragment =
            (AppsListFragment) fragment.getChildFragmentManager().getPrimaryNavigationFragment();
        if (appsListFragment != null) {
          //appsListFragment.preparePDF();
          appsListFragment.prepareTXT();
        }
      }
    });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case 100:
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
          // User checks permission.

        } else {
          Toast.makeText(MainActivity.this, "Permission is denied.", Toast.LENGTH_SHORT).show();
          finish();
        }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    SearchManager searchManager = (SearchManager)
        getSystemService(Context.SEARCH_SERVICE);
    searchMenuItem = menu.findItem(R.id.search);
    searchView = (SearchView) searchMenuItem.getActionView();

    if (searchManager == null)
      return false;
    searchView.setSearchableInfo(searchManager.
        getSearchableInfo(getComponentName()));
    searchView.setSubmitButtonEnabled(true);
    searchView.setOnQueryTextListener(this);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_refresh) {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
      if (fragment == null)
        return false;
      AppsListFragment appsListFragment =
          (AppsListFragment) fragment.getChildFragmentManager().getPrimaryNavigationFragment();
      if (appsListFragment != null) {
        appsListFragment.Refresh();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    if (fragment == null)
      return false;
    AppsListFragment appsListFragment =
        (AppsListFragment) fragment.getChildFragmentManager().getPrimaryNavigationFragment();
    if (appsListFragment != null) {
      appsListFragment.SetFilter(newText);
    }

    return true;
  }
}