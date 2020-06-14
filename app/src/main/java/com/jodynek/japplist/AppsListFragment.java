package com.jodynek.japplist;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AppsListFragment extends Fragment {
  SwipeRefreshLayout pullToRefresh;
  ListView userInstalledApps;
  private List<AppList> installedApps;
  private List<AppList> filteredInstalledApps;
  private AppAdapter installedAppAdapter;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  ) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_apps_list, container, false);
  }

  private List<AppList> getInstalledApps() {
    if (getContext() == null)
      return null;
    PackageManager pm = getContext().getPackageManager();
    List<AppList> apps = new ArrayList<>();
    List<PackageInfo> packs = getContext().getPackageManager().getInstalledPackages(0);

    for (int i = 0; i < packs.size(); i++) {
      PackageInfo p = packs.get(i);
      if ((!isSystemPackage(p))) {
        String appName = p.applicationInfo.loadLabel(getContext().getPackageManager()).toString();
        Drawable icon = p.applicationInfo.loadIcon(getContext().getPackageManager());
        String packages = p.applicationInfo.packageName;
        long size = getPackageSizeInfo(packages);
        String sizeMB = FormatSize(size);
        apps.add(new AppList(appName, icon, packages, size, sizeMB));
      }
    }

    // sorting collection...
    Collections.sort(apps, new Comparator<AppList>() {
      @Override
      public int compare(AppList o1, AppList o2) {
        return o1.name.compareTo(o2.name);
      }
    });
    return apps;
  }

  private boolean isSystemPackage(PackageInfo pkgInfo) {
    return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  private long getPackageSizeInfo(String packageName) {
    if (getContext() == null)
      return 0;
    final StorageStatsManager storageStatsManager = (StorageStatsManager) getContext().
        getSystemService(Context.STORAGE_STATS_SERVICE);
    final StorageManager storageManager = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
    try {

      ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(packageName, 0);
      if (storageStatsManager == null)
        return 0;
      StorageStats storageStats = storageStatsManager.queryStatsForUid(ai.storageUuid, ai.uid);
      long cacheSize = storageStats.getCacheBytes();
      long dataSize = storageStats.getDataBytes();
      long apkSize = storageStats.getAppBytes();

      return dataSize + cacheSize + apkSize;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  private String FormatSize(long size) {
    String sizeString;
    if (size < 1000)
      sizeString = String.valueOf(size);
    else if (size < 1000000)
      sizeString = size / 1024 + "kB";
    else if (size < 1000000000L) {
      sizeString = size / 1024 / 1024 + "MB";
    } else
      sizeString = size / 1024 / 1024 / 1024 + "GB";

    return sizeString;
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    Refresh();

    super.onViewCreated(view, savedInstanceState);
  }

  public void Refresh() {
    if (getView() == null)
      return;
    pullToRefresh = getView().findViewById(R.id.pullToRefresh);
    pullToRefresh.setRefreshing(true);
    //setting an setOnRefreshListener on the SwipeDownLayout
    pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

      @Override
      public void onRefresh() {
        Refresh();
      }
    });
    userInstalledApps = getView().findViewById(R.id.installed_app_list);
    installedApps = getInstalledApps();
    if (this.getContext() == null)
      return;
    installedAppAdapter = new AppAdapter(this.getContext(), installedApps);
    userInstalledApps.setAdapter(installedAppAdapter);
    userInstalledApps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {

        String[] colors = {" Open App", " App Info"};
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Choose Action")
            .setItems(colors, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position of the selected item
                if (which == 0) {
                  if (getContext() == null)
                    return;
                  Intent intent = getContext().getPackageManager().
                      getLaunchIntentForPackage(installedApps.get(i).packages);
                  if (intent != null) {
                    startActivity(intent);
                  } else {
                    Toast.makeText(getContext(),
                        installedApps.get(i).packages + " Error, Please Try Again...",
                        Toast.LENGTH_SHORT).show();
                  }
                }
                if (which == 1) {
                  Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                  intent.setData(Uri.parse("package:" + installedApps.get(i).packages));
                  Toast.makeText(getContext(), installedApps.get(i).packages, Toast.LENGTH_SHORT).show();
                  startActivity(intent);
                }
              }
            });
        builder.show();
      }
    });

    //Total Number of Installed-Apps(i.e. List Size)
    String abc = userInstalledApps.getCount() + "";
    TextView countApps = getView().findViewById(R.id.countApps);
    countApps.setText(getResources().getString(R.string.total_installed_apps, abc));
    pullToRefresh.setRefreshing(false);
    Toast.makeText(getContext(), abc + getResources().getString(R.string.apps),
        Toast.LENGTH_SHORT).show();
  }

  public void SetFilter(String filter) {
    installedAppAdapter.getFilter().filter(filter);
  }

  // TXT export file
  public void prepareTXT() {
    String filePath = "/storage/emulated/0/jAppList/AppsList.txt";
    File txtFile = new File(filePath);
    //Create parent directories
    File parentFile = txtFile.getParentFile();
    if (!parentFile.exists() && !parentFile.mkdirs()) {
      throw new IllegalStateException("Couldn't create directory: " + parentFile);
    }
    boolean fileExists = txtFile.exists();
    // If File already Exists. delete it.
    if (fileExists) {
      fileExists = !txtFile.delete();
    }
    try {
      if (!fileExists) {
        // Create New File.
        fileExists = txtFile.createNewFile();
      }
      if (fileExists) {
        generateTXT(txtFile);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  private void generateTXT(File txtFile) {
    try {
      Date date = Calendar.getInstance().getTime();
      SimpleDateFormat simpleDate = new SimpleDateFormat("dd.MM.yyyy");
      String sText = "Android Applications List - " + simpleDate.format(date) + "\n\n";

      formatAppName();
      formatSize();
      FileWriter writer = new FileWriter(txtFile);
      for (AppList appList : installedApps) {
        sText += appList.getName() + " " + appList.getSizeMB() + ", " +
            appList.getPackages() + "\n";
      }
      writer.append(sText);
      writer.flush();
      writer.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    Toast.makeText(getContext(), "Saved - " + txtFile.getAbsolutePath(),
        Toast.LENGTH_SHORT).show();
  }

  private void formatSize() {
    int maxAppLengthSize = 0;
    for (AppList appList : installedApps) {
      String sizeMB = appList.sizeMB;
      if (sizeMB.length() > maxAppLengthSize)
        maxAppLengthSize = sizeMB.length();
    }

    for (AppList appList : installedApps) {
      String sizeMB = appList.sizeMB;
      int lengthDiff = maxAppLengthSize - sizeMB.length();
      for (int i = 0; i < lengthDiff; i++) {
        sizeMB = " " + sizeMB;
      }
      appList.setSizeMB(sizeMB);
    }
  }

  private void formatAppName() {
    int maxAppLengthName = 0;
    for (AppList appList : installedApps) {
      if (appList.getName().length() > maxAppLengthName)
        maxAppLengthName = appList.getName().length();
    }

    for (AppList appList : installedApps) {
      String appName = appList.getName();
      int lengthDiff = maxAppLengthName - appName.length();
      for (int i = 0; i < lengthDiff; i++) {
        appName += " ";
      }
      appList.setName(appName);
    }
  }

  // PDF creation interface
  public void preparePDF() {
    if (getView() == null)
      return;
    ListView lst = getView().findViewById(R.id.installed_app_list);

    String filePath = "/storage/emulated/0/jAppList/AppsList.pdf";
    File pdfFile = new File(filePath);
    //Create parent directories
    File parentFile = pdfFile.getParentFile();
    if (!parentFile.exists() && !parentFile.mkdirs()) {
      throw new IllegalStateException("Couldn't create directory: " + parentFile);
    }
    boolean fileExists = pdfFile.exists();
    // If File already Exists. delete it.
    if (fileExists) {
      fileExists = !pdfFile.delete();
    }
    try {
      if (!fileExists) {
        // Create New File.
        fileExists = pdfFile.createNewFile();
      }
      if (fileExists) {
        generatePDF(pdfFile);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  private void generatePDF(File file) {
    // create a new document
    PdfDocument document = new PdfDocument();
    // crate a page description
    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
    // start a page
    PdfDocument.Page page = document.startPage(pageInfo);
    Canvas canvas = page.getCanvas();
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    canvas.drawCircle(50, 50, 30, paint);
    paint.setColor(Color.BLACK);
    canvas.drawText("Petr Jodas", 80, 50, paint);
    //canvas.draw
    // finish the page
    document.finishPage(page);
    // draw text on the graphics object of the page
    // Create Page 2
    pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 2).create();
    page = document.startPage(pageInfo);
    canvas = page.getCanvas();
    paint = new Paint();
    paint.setColor(Color.BLUE);
    canvas.drawCircle(100, 100, 100, paint);
    document.finishPage(page);

    try {
      document.writeTo(new FileOutputStream(file.getAbsolutePath()));
      Toast.makeText(getContext(), "Done", Toast.LENGTH_LONG).show();
    } catch (IOException e) {
      Log.e("main", "error " + e.toString());
      Toast.makeText(getContext(), "Something wrong: " + e.toString(), Toast.LENGTH_LONG).show();
    }
    // close the document
    document.close();
  }


  public class AppAdapter extends BaseAdapter implements Filterable {
    public LayoutInflater layoutInflater;
    public List<AppList> listStorage;
    private AppFilter appFilter;

    public AppAdapter(Context context, List<AppList> customizedListView) {
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      listStorage = customizedListView;
    }

    @Override
    public Filter getFilter() {
      if (appFilter == null) {
        appFilter = new AppFilter();
      }

      return appFilter;
    }

    @Override
    public int getCount() {
      return listStorage.size();
    }

    @Override
    public Object getItem(int position) {
      return position;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder listViewHolder;
      if (convertView == null) {
        listViewHolder = new ViewHolder();
        convertView = layoutInflater.inflate(R.layout.installed_app_list, parent, false);
        listViewHolder.textInListView = convertView.findViewById(R.id.list_app_name);
        listViewHolder.imageInListView = convertView.findViewById(R.id.app_icon);
        listViewHolder.packageInListView = convertView.findViewById(R.id.app_package);
        listViewHolder.sizeInListView = convertView.findViewById(R.id.app_size);
        convertView.setTag(listViewHolder);
      } else {
        listViewHolder = (ViewHolder) convertView.getTag();
      }
      listViewHolder.textInListView.setText(listStorage.get(position).getName());
      listViewHolder.imageInListView.setImageDrawable(listStorage.get(position).getIcon());
      listViewHolder.packageInListView.setText(listStorage.get(position).getPackages());
      listViewHolder.sizeInListView.setText(listStorage.get(position).getSizeMB());

      return convertView;
    }

    class ViewHolder {
      TextView textInListView;
      ImageView imageInListView;
      TextView packageInListView;
      TextView sizeInListView;
    }
  }

  public class AppList {
    Drawable icon;
    private String name;
    private String packages;
    private long size;
    private String sizeMB;

    public AppList(String name, Drawable icon, String packages, long size, String sizeMB) {
      this.name = name;
      this.icon = icon;
      this.packages = packages;
      this.size = size;
      this.sizeMB = sizeMB;
    }

    public String getName() {
      return name;
    }

    public void setName(String value) {
      name = value;
    }

    public Drawable getIcon() {
      return icon;
    }

    public String getPackages() {
      return packages;
    }

    public long getSize() {
      return size;
    }

    public String getSizeMB() {
      return sizeMB;
    }

    public void setSizeMB(String value) {
      sizeMB = value;
    }
  }

  private class AppFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults filterResults = new FilterResults();
      if (constraint != null && constraint.length() > 0) {
        ArrayList<AppList> tempList = new ArrayList<>();

        // search content in apps list
        for (AppList appList : installedApps) {
          if (appList.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
            tempList.add(appList);
          }
        }

        filterResults.count = tempList.size();
        filterResults.values = tempList;
      } else {
        filterResults.count = installedApps.size();
        filterResults.values = installedApps;
      }

      return filterResults;
    }

    /**
     * Notify about filtered list to ui
     *
     * @param constraint text
     * @param results    filtered result
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      if (installedApps == null) {
        List<AppList> installedApps = new ArrayList<>();
      }
      filteredInstalledApps = (ArrayList<AppList>) results.values;
      installedAppAdapter.listStorage = filteredInstalledApps;
      if (getView() == null)
        return;
      TextView countApps = getView().findViewById(R.id.countApps);
      countApps.setText(getResources().getString(R.string.total_installed_apps,
          String.valueOf(filteredInstalledApps.size())));
      installedAppAdapter.notifyDataSetChanged();
    }
  }

}