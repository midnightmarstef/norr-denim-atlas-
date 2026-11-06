package is.norr.atlas;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Thin shell around the NORR Denim Atlas page bundled in assets/index.html. */
public class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 1001;
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

        if (savedState != null) web.restoreState(savedState);
        else web.loadUrl("file:///android_asset/index.html");
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
