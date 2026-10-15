package com.support.webapp_to_webview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final long RETRY_INITIAL_DELAY_MS = 2_000;
    private static final long RETRY_MAX_DELAY_MS = 60_000;
    private WebView myWebView;
    ProgressBar progressBar;

    // Kiosk apps are often launched at boot, before the network is up, and can't be
    // reopened in lock task mode, so a failed page load is retried until it succeeds.
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private boolean loadFailed;
    private String failedUrl;
    private int retryAttempt;

    private final Runnable retryLoad = new Runnable() {
        @Override
        public void run() {
            if (loadFailed && failedUrl != null) {
                Log.i(TAG, "Reloading " + failedUrl);
                // Cleared here rather than in onPageStarted: WebView reports a failed
                // navigation's onReceivedError before its onPageStarted/onPageFinished.
                loadFailed = false;
                myWebView.loadUrl(failedUrl);
            }
        }
    };

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        private boolean validated;

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            boolean nowValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            // Capabilities also change on signal strength updates; only act when the
            // network becomes validated, otherwise the retry backoff is bypassed.
            if (nowValidated && !validated) {
                // Called on the ConnectivityThread; retry on the main thread right away.
                retryHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (loadFailed && !isDestroyed()) {
                            Log.i(TAG, "Network validated, retrying now");
                            retryAttempt = 0;
                            retryHandler.removeCallbacks(retryLoad);
                            retryLoad.run();
                        }
                    }
                });
            }
            validated = nowValidated;
        }

        @Override
        public void onLost(@NonNull Network network) {
            validated = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startWebView("https://ap-iwa.com/ProMobileV3"); // Update me to the URL
        registerNetworkCallback();
    }

    @Override
    protected void onDestroy() {
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        retryHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            cancelRetry();
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private static boolean isRetryable(WebResourceRequest request, WebResourceError error) {
        String scheme = request.getUrl().getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }
        switch (error.getErrorCode()) {
            case WebViewClient.ERROR_HOST_LOOKUP:
            case WebViewClient.ERROR_CONNECT:
            case WebViewClient.ERROR_TIMEOUT:
            case WebViewClient.ERROR_IO:
            case WebViewClient.ERROR_UNKNOWN:
                return true;
            default:
                return false;
        }
    }

    private void cancelRetry() {
        loadFailed = false;
        retryHandler.removeCallbacks(retryLoad);
    }

    private void scheduleRetry() {
        long delayMs = Math.min(RETRY_INITIAL_DELAY_MS << Math.min(retryAttempt, 5), RETRY_MAX_DELAY_MS);
        retryAttempt++;
        Log.i(TAG, "Retrying " + failedUrl + " in " + delayMs + "ms (attempt " + retryAttempt + ")");
        retryHandler.removeCallbacks(retryLoad);
        retryHandler.postDelayed(retryLoad, delayMs);
    }

    private void startWebView(String url) {

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        myWebView= findViewById(R.id.webview);
        WebSettings webSettings=myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                cancelRetry();
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (loadFailed && !url.equals(failedUrl)) {
                    // Navigated elsewhere; drop the pending retry so it doesn't pull
                    // the user off the new page.
                    cancelRetry();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!loadFailed) {
                    retryAttempt = 0;
                    retryHandler.removeCallbacks(retryLoad);
                }
                if (progressBar.isShown()) {
                    progressBar.setVisibility(View.GONE);
                }
                suppressVirtualKeyboard(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) {
                    return;
                }
                Log.e(TAG, "onReceivedError: code=" + error.getErrorCode()
                        + " url=" + request.getUrl() + " desc=" + error.getDescription());
                if (retryAttempt == 0) {
                    Toast.makeText(MainActivity.this, "Error:" + error.getDescription(), Toast.LENGTH_SHORT).show();
                }
                if (isRetryable(request, error)) {
                    loadFailed = true;
                    failedUrl = request.getUrl().toString();
                    scheduleRetry();
                }
            }

        });
        myWebView.loadUrl(url);
    }

    private void suppressVirtualKeyboard(WebView view) {
        String js =
            "(function() {" +
            "  function noKeyboard(el) {" +
            "    el.setAttribute('inputmode', 'none');" +
            "  }" +
            "  document.querySelectorAll('input, textarea').forEach(noKeyboard);" +
            "  new MutationObserver(function(mutations) {" +
            "    mutations.forEach(function(m) {" +
            "      m.addedNodes.forEach(function(n) {" +
            "        if (n.nodeType !== 1) return;" +
            "        if (n.matches('input, textarea')) noKeyboard(n);" +
            "        n.querySelectorAll('input, textarea').forEach(noKeyboard);" +
            "      });" +
            "    });" +
            "  }).observe(document.documentElement, { childList: true, subtree: true });" +
            "})();";
        view.evaluateJavascript(js, null);
    }
}
