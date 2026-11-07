package is.norr.atlas;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Thin shell around the NORR Denim Atlas page bundled in assets/index.html. */
public class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 1001;
    private static final String VERSION_URL =
            "https://github.com/midnightmarstef/norr-denim-atlas-/releases/latest/download/version.json";
    private static final String APK_URL =
            "https://github.com/midnightmarstef/norr-denim-atlas-/releases/latest/download/norr-atlas.apk";
    private WebView web;
    private ValueCallback<Uri[]> pendingPhotos;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        web = new WebView(this);
        setContentView(web);
        web.setBackgroundColor(Color.parseColor("#0b1222"));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);        // saved listings + cached routes live in localStorage
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if ("file".equals(url.getScheme())) return false;          // the atlas itself
                try { startActivity(new Intent(Intent.ACTION_VIEW, url)); } // portals, Booking, websites
                catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingPhotos != null) pendingPhotos.onReceiveValue(null);
                pendingPhotos = callback;
                Intent intent = params.createIntent();
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(intent, PICK_PHOTOS); }
                catch (Exception e) { pendingPhotos = null; return false; }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "NorrApp");

        if (savedState != null) web.restoreState(savedState);
        else { web.loadUrl("file:///android_asset/index.html"); checkForUpdate(); }
    }

    /** Called from the page: Export writes the listings file to Downloads. */
    public final class Bridge {
        @JavascriptInterface
        public String saveText(String filename, String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    v.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return "";
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) { out.write(bytes); }
                    toast("Saved " + filename + " to Downloads");
                    return "Downloads/" + filename;
                } else {
                    File dir = getExternalFilesDir(null);
                    File f = new File(dir, filename);
                    try (FileOutputStream out = new FileOutputStream(f)) { out.write(bytes); }
                    toast("Saved " + f.getAbsolutePath());
                    return f.getAbsolutePath();
                }
            } catch (Exception e) {
                toast("Could not save the export");
                return "";
            }
        }

        @JavascriptInterface
        public String version() {
            PackageInfo p = packageInfo();
            return p == null ? "" : p.versionName;
        }
    }

    private void toast(String msg) { runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show()); }

    private PackageInfo packageInfo() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("deprecation")
    private long installedVersionCode() {
        PackageInfo p = packageInfo();
        if (p == null) return Long.MAX_VALUE;
        return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
    }

    /** Once per launch: ask GitHub for the newest build and offer it if it is newer than this one. */
    private void checkForUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(VERSION_URL).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setInstanceFollowRedirects(true);
                if (c.getResponseCode() != 200) return;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (InputStream in = c.getInputStream()) {
                    byte[] b = new byte[4096]; int n;
                    while ((n = in.read(b)) > 0) buf.write(b, 0, n);
                }
                JSONObject j = new JSONObject(buf.toString("UTF-8"));
                long latest = j.optLong("versionCode", 0);
                if (latest <= installedVersionCode()) return;
                String name = j.optString("versionName", "");
                String apk = j.optString("apk", APK_URL);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    new AlertDialog.Builder(this)
                            .setTitle("Update ready")
                            .setMessage("NORR Atlas " + name + " is available. Download it, then tap the file to install. Your saved listings stay on the phone.")
                            .setPositiveButton("Download", (d, w) -> {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk))); } catch (Exception ignored) { }
                            })
                            .setNegativeButton("Later", null)
                            .show();
                });
            } catch (Exception ignored) { }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTOS && pendingPhotos != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    result = new Uri[n];
                    for (int i = 0; i < n; i++) result[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    result = new Uri[]{ data.getData() };
                }
            }
            pendingPhotos.onReceiveValue(result);
            pendingPhotos = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        // Let the page close its detail panel first; leave the app only when nothing is open.
        web.evaluateJavascript("(function(){return !!(window.__back&&window.__back())})()",
                value -> { if (!"true".equals(value)) finish(); });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }
}
