package com.generated.app;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private ListView wifiListView;
    private List<ScanResult> wifiScanList;
    private ArrayAdapter<String> wifiListAdapter;
    private LinearLayout passwordInputLayout;
    private EditText passwordEditText;
    private Button connectButton;
    private String selectedSSID;
    private WifiReceiver wifiReceiver;

    // Color palette
    private final int COLOR_PRIMARY = 0xFF4CAF50; // Green
    private final int COLOR_ACCENT = 0xFFFFC107; // Amber
    private final int COLOR_BACKGROUND = 0xFFF5F5F5; // Light Gray
    private final int COLOR_SURFACE = 0xFFFFFFFF; // White
    private final int COLOR_ON_SURFACE = 0xFF212121; // Dark Gray
    private final int COLOR_ON_PRIMARY = 0xFFFFFFFF; // White
    private final int COLOR_BUTTON_DISABLED = 0xFFBDBDBD; // Medium Gray
    private final int COLOR_DIVIDER = 0xFFE0E0E0; // Light Gray for dividers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeWifiManager();
            startWifiScan();
        }
    }

    private void setupUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainLayout.setBackgroundColor(COLOR_BACKGROUND);

        TextView titleTextView = new TextView(this);
        titleTextView.setText("רשתות WiFi זמינות");
        titleTextView.setTextSize(24);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setTextColor(COLOR_ON_SURFACE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dpToPx(16);
        titleTextView.setLayoutParams(titleParams);
        mainLayout.addView(titleTextView);

        wifiListView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        listParams.setMargins(0, 0, 0, dpToPx(16));
        wifiListView.setLayoutParams(listParams);
        wifiListView.setDividerHeight(1);
        Drawable dividerDrawable = new GradientDrawable();
        ((GradientDrawable)dividerDrawable).setColor(COLOR_DIVIDER);
        wifiListView.setDivider(dividerDrawable);
        wifiListView.setCacheColorHint(android.R.color.transparent); // Prevent scrolling lag
        mainLayout.addView(wifiListView);

        passwordInputLayout = new LinearLayout(this);
        passwordInputLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLayoutParams.topMargin = dpToPx(8);
        passwordInputLayout.setLayoutParams(inputLayoutParams);
        passwordInputLayout.setVisibility(View.GONE);
        passwordInputLayout.setGravity(Gravity.CENTER_VERTICAL);
        passwordInputLayout.setBaselineAligned(false);
        passwordInputLayout.setBackgroundColor(COLOR_SURFACE);
        passwordInputLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        passwordInputLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        passwordInputLayout.setElevation(dpToPx(4)); // Subtle shadow

        passwordEditText = new EditText(this);
        passwordEditText.setHint("הכנס סיסמה");
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setHintTextColor(0xFF9E9E9E); // Lighter gray for hint
        LinearLayout.LayoutParams passwordEditParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        passwordEditParams.setMargins(0, 0, dpToPx(16), 0);
        passwordEditText.setLayoutParams(passwordEditParams);
        passwordEditText.setBackground(createRoundedBackground(COLOR_DIVIDER, 8));
        passwordEditText.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        passwordInputLayout.addView(passwordEditText);

        connectButton = new Button(this);
        connectButton.setText("התחבר");
        connectButton.setTextSize(16);
        connectButton.setTextColor(COLOR_ON_PRIMARY);
        connectButton.setBackground(createRoundedBackground(COLOR_PRIMARY, 8));
        connectButton.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToWifi();
            }
        });
        passwordInputLayout.addView(connectButton);

        mainLayout.addView(passwordInputLayout);

        setContentView(mainLayout);

        wifiScanList = new ArrayList<>();
        wifiListAdapter = new ArrayAdapter<String>(this, R.layout.list_item_wifi, R.id.ssidTextView, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView ssidTextView = view.findViewById(R.id.ssidTextView);
                TextView securityTextView = view.findViewById(R.id.securityTextView);

                ScanResult result = wifiScanList.get(position);
                String capabilities = result.capabilities;
                String security = getSecurityType(capabilities);
                String signalStrength = getSignalStrength(result.level);

                ssidTextView.setText(result.SSID);
                securityTextView.setText(signalStrength + " (" + security + ")");

                return view;
            }
        };
        wifiListView.setAdapter(wifiListAdapter);

        wifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScanResult selectedScan = wifiScanList.get(position);
                selectedSSID = selectedScan.SSID;

                boolean isSecured = !isNetworkOpen(selectedScan.capabilities);
                if (isSecured) {
                    passwordInputLayout.setVisibility(View.VISIBLE);
                    passwordEditText.setText(""); // Clear previous password
                } else {
                    passwordInputLayout.setVisibility(View.GONE);
                    connectToWifi(selectedSSID, ""); // Connect to open network
                }
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private Drawable createRoundedBackground(int color, int radius) {
        GradientDrawable shapeDrawable = new GradientDrawable();
        shapeDrawable.setShape(GradientDrawable.RECTANGLE);
        shapeDrawable.setColor(color);
        shapeDrawable.setCornerRadius(dpToPx(radius));
        return shapeDrawable;
    }

    private boolean checkPermissions() {
        int coarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int fineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        int wifiStatePermission = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);
        int changeWifiStatePermission = checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE);
        return coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
               fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
               wifiStatePermission == PackageManager.PERMISSION_GRANTED &&
               changeWifiStatePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
        requestPermissions(permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                initializeWifiManager();
                startWifiScan();
            } else {
                Toast.makeText(this, "נדרשות הרשאות לסריקה והתחברות לרשתות WiFi.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeWifiManager() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(this, "שירות WiFi אינו זמין", Toast.LENGTH_LONG).show();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "מפעיל WiFi...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
            wifiReceiver = new WifiReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                        if (state == WifiManager.WIFI_STATE_ENABLED) {
                            Toast.makeText(MainActivity.this, "WiFi הופעל. סורק רשתות...", Toast.LENGTH_SHORT).show();
                            startWifiScan();
                            unregisterReceiver(this);
                        } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                             Toast.makeText(MainActivity.this, "WiFi לא הופעל.", Toast.LENGTH_LONG).show();
                             unregisterReceiver(this);
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wifiReceiver, filter);
        } else {
            startWifiScan();
        }
    }

    private void startWifiScan() {
        if (wifiManager == null) return;

        wifiReceiver = new WifiReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        processWifiScanResults();
                    } else {
                        Toast.makeText(MainActivity.this, "סריקת WiFi נכשלה.", Toast.LENGTH_SHORT).show();
                    }
                    unregisterReceiver(this);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReceiver, intentFilter);

        boolean startScan = wifiManager.startScan();
        if (!startScan) {
            Toast.makeText(this, "לא ניתן להתחיל סריקת WiFi.", Toast.LENGTH_SHORT).show();
            unregisterReceiver(wifiReceiver);
        }
    }

    private void processWifiScanResults() {
        wifiScanList.clear();
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    wifiScanList.add(result);
                }
            }
        }
        updateWifiList();
    }

    private void updateWifiList() {
        wifiListAdapter.clear();
        for (ScanResult result : wifiScanList) {
            wifiListAdapter.add(result.SSID); // Add only SSID, details handled in getView
        }
        wifiListAdapter.notifyDataSetChanged();
    }

    private String getSecurityType(String capabilities) {
        if (capabilities.contains("WEP")) {
            return "WEP";
        } else if (capabilities.contains("WPA3")) {
            return "WPA3";
        } else if (capabilities.contains("WPA2")) {
            return "WPA2";
        } else if (capabilities.contains("WPA")) {
            return "WPA";
        } else {
            return "פתוחה";
        }
    }

    private String getSignalStrength(int level) {
        if (level >= -50) {
            return "חזק";
        } else if (level >= -70) {
            return "בינוני";
        } else {
            return "חלש";
        }
    }

    private void connectToWifi(String ssid, String password) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + ssid + "\"";

        boolean isSecured = !password.isEmpty();
        if (isSecured) {
            wifiConfiguration.preSharedKey = "\"" + password + "\"";
            String security = getSecurityType(getScanResultCapabilities(ssid)); // Determine security type

            if ("WPA3".equals(security)) {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            } else if ("WPA2".equals(security)) {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            } else if ("WPA".equals(security)) {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            } else if ("WEP".equals(security)) {
                wifiConfiguration.wepKeys[0] = "\"" + password + "\"";
                wifiConfiguration.wepTxKeyIndex = 0;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                 // Default to WPA2 PSK if password is provided but security type is unknown/open
                 wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                 wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                 wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                 wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            }
        } else {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        int netId = -1;
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration config : existingConfigs) {
                if (config.SSID.equals("\"" + ssid + "\"")) {
                    netId = config.networkId;
                    if (isSecured && !password.isEmpty()) {
                        config.preSharedKey = "\"" + password + "\"";
                        wifiManager.updateNetwork(config);
                    } else if (!isSecured && config.preSharedKey != null) {
                        config.preSharedKey = null;
                        wifiManager.updateNetwork(config);
                    }
                    break;
                }
            }
        }

        if (netId == -1) {
            netId = wifiManager.addNetwork(wifiConfiguration);
        }

        if (netId != -1) {
            boolean disconnected = wifiManager.disconnect();
            boolean enableNetwork = wifiManager.enableNetwork(netId, true);
            boolean saved = wifiManager.saveConfiguration();

            if (enableNetwork) {
                Toast.makeText(this, "מנסה להתחבר ל-" + ssid + "...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "נכשל בניסיון להתחבר לרשת " + ssid, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "נכשל בהוספת/קונפיגורציית רשת עבור " + ssid, Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to get capabilities for a given SSID (simplistic, assumes first match)
    private String getScanResultCapabilities(String ssid) {
        for (ScanResult result : wifiScanList) {
            if (result.SSID.equals(ssid)) {
                return result.capabilities;
            }
        }
        return ""; // Not found
    }

    private void connectToWifi() {
        String password = passwordEditText.getText().toString();
        if (selectedSSID != null) {
            connectToWifi(selectedSSID, password);
        } else {
            Toast.makeText(this, "אנא בחר רשת תחילה.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wifiManager != null && checkPermissions()) {
             if (!wifiManager.isWifiEnabled()) {
                 initializeWifiManager();
             } else {
                 startWifiScan();
             }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wifiReceiver != null) {
            try {
                unregisterReceiver(wifiReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered, ignore
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiReceiver != null) {
            try {
                unregisterReceiver(wifiReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered, ignore
            }
        }
    }

    public static boolean isNetworkOpen(String capabilities) {
        return capabilities.contains("ESS") &&
               !capabilities.contains("WEP") &&
               !capabilities.contains("WPA") &&
               !capabilities.contains("WPA2") &&
               !capabilities.contains("WPA3");
    }
}
