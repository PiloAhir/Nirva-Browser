package com.nirva.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout root;
    private LinearLayout toolbar;
    private EditText addressBar;
    private ProgressBar progressBar;
    private TextView statusText;

    private final List<WebView> tabs = new ArrayList<>();
    private final List<String> bookmarks = new ArrayList<>();
    private final List<String> history = new ArrayList<>();

    private int currentTab = 0;
    private boolean privateMode = false;
    private boolean desktopMode = false;
    private boolean darkMode = false;

    private WebView web;

    private final String HOME_URL = "https://www.google.com";

    private final String[] trackerHosts = {
            "doubleclick.net",
            "google-analytics.com",
            "googletagmanager.com",
            "facebook.net",
            "connect.facebook.net",
            "adservice.google.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createBrowserUI();
        addNewTab(HOME_URL);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button makeButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(6), 0, dp(6), 0);

        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void createBrowserUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);

        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), dp(3), dp(4), dp(3));
        toolbar.setBackgroundColor(0xFFF1F3F4);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setHint("Search or enter website");
        addressBar.setPadding(dp(10), 0, dp(8), 0);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterPressed = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                loadAddress(addressBar.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        toolbar.addView(addressBar, new LinearLayout.LayoutParams(
                0, dp(46), 1f));

        toolbar.addView(makeButton("Go", () ->
                loadAddress(addressBar.getText().toString())));

        root.addView(toolbar);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(2), 0, dp(2), 0);

        controls.addView(makeButton("‹", () -> {
            if (web != null && web.canGoBack()) web.goBack();
        }));

        controls.addView(makeButton("›", () -> {
            if (web != null && web.canGoForward()) web.goForward();
        }));

        controls.addView(makeButton("↻", () -> {
            if (web != null) web.reload();
        }));

        controls.addView(makeButton("Home", () -> loadAddress(HOME_URL)));

        controls.addView(makeButton("＋ Tab", () -> addNewTab(HOME_URL)));

        controls.addView(makeButton("Tabs", this::showTabs));

        controls.addView(makeButton("Menu", this::showBrowserMenu));

        root.addView(controls);

        progressBar = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        root.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        statusText = new TextView(this);
        statusText.setTextSize(11);
        statusText.setPadding(dp(7), dp(2), dp(7), dp(2));
        statusText.setSingleLine(true);
        statusText.setText("Nirva Browser");
        root.addView(statusText);

        setContentView(root);
    }

    private WebView createWebView() {
        WebView view = new WebView(this);

        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        if (desktopMode) {
            settings.setUserAgentString(
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        } else {
            settings.setUserAgentString(null);
        }

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                progressBar.setVisibility(
                        progress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (view == web && title != null) {
                    statusText.setText(title);
                }
            }
        });

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if (scheme != null
                        && !scheme.equalsIgnoreCase("http")
                        && !scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    } catch (Exception ignored) {
                        Toast.makeText(MainActivity.this,
                                "આ લિંક ખોલી શકાતી નથી",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                return false;
            }

            @Override
            public void onPageStarted(
                    WebView view, String url, Bitmap favicon) {
                if (view == web) {
                    addressBar.setText(url);
                    statusText.setText("Loading...");
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == web) {
                    addressBar.setText(url);
                    statusText.setText(view.getTitle() == null
                            ? url : view.getTitle());

                    if (!privateMode && url != null
                            && url.startsWith("http")) {
                        if (!history.contains(url)) {
                            history.add(url);
                        }
                    }
                }
            }
        });

        view.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength) {
                downloadFile(url, userAgent, contentDisposition, mimeType);
            }
        });

        view.setOnLongClickListener(v -> {
            WebView.HitTestResult result = view.getHitTestResult();

            if (result == null) return false;

            int type = result.getType();
            String targetUrl = result.getExtra();

            boolean isMedia =
        type == WebView.HitTestResult.IMAGE_TYPE
        || type == WebView.HitTestResult.IMAGE_ANCHOR_TYPE
        || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        || type == WebView.HitTestResult.SRC_ANCHOR_TYPE
        || type == WebView.HitTestResult.ANCHOR_TYPE;

            if (isMedia && targetUrl != null
                    && !targetUrl.trim().isEmpty()) {
                showMediaOptions(targetUrl);
                return true;
            }

            return false;
        });

        return view;
    }

    private void addNewTab(String url) {
        WebView newWeb = createWebView();

        tabs.add(newWeb);
        currentTab = tabs.size() - 1;

        web = newWeb;

        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        web.setVisibility(View.VISIBLE);
        addressBar.setText(url);

        web.loadUrl(url);
        updateTabStatus();
    }

    private void switchTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        if (web != null) {
            web.setVisibility(View.GONE);
        }

        currentTab = index;
        web = tabs.get(index);

        if (web.getParent() != root) {
            if (web.getParent() instanceof ViewGroup) {
                ((ViewGroup) web.getParent()).removeView(web);
            }
            root.addView(web, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        web.setVisibility(View.VISIBLE);
        addressBar.setText(web.getUrl() == null ? "" : web.getUrl());
        updateTabStatus();
    }

    private void showTabs() {
        if (tabs.isEmpty()) {
            addNewTab(HOME_URL);
            return;
        }

        String[] labels = new String[tabs.size() + 1];

        for (int i = 0; i < tabs.size(); i++) {
            WebView item = tabs.get(i);
            String title = item.getTitle();

            if (title == null || title.trim().isEmpty()) {
                title = item.getUrl() == null ? "New tab" : item.getUrl();
            }

            labels[i] = (i == currentTab ? "✓ " : "") + title;
        }

        labels[tabs.size()] = "+ New tab";

        new AlertDialog.Builder(this)
                .setTitle("Tabs (" + tabs.size() + ")")
                .setItems(labels, (dialog, which) -> {
                    if (which == tabs.size()) {
                        addNewTab(HOME_URL);
                    } else {
                        switchTab(which);
                    }
                })
                .setNeutralButton("Close current tab", (dialog, which) ->
                        closeCurrentTab())
                .show();
    }

    private void closeCurrentTab() {
        if (tabs.size() <= 1) {
            if (web != null) web.loadUrl(HOME_URL);
            return;
        }

        WebView old = tabs.remove(currentTab);
        root.removeView(old);
        old.stopLoading();
        old.destroy();

        currentTab = Math.max(0, currentTab - 1);
        web = tabs.get(currentTab);
        web.setVisibility(View.VISIBLE);
        addressBar.setText(web.getUrl() == null ? "" : web.getUrl());

        updateTabStatus();
    }

    private void updateTabStatus() {
        statusText.setText("Nirva Browser • "
                + (currentTab + 1) + "/" + tabs.size() + " tabs"
                + (privateMode ? " • Private" : ""));
    }

    private void loadAddress(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String text = input.trim();

        if (text.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            if (web != null) web.loadUrl(text);
            return;
        }

        if (text.contains(" ") || !text.contains(".")) {
            loadSearch(text);
            return;
        }

        if (web != null) {
            web.loadUrl("https://" + text);
        }
    }

    private void loadSearch(String query) {
        String encoded = Uri.encode(query);
        if (web != null) {
            web.loadUrl("https://www.google.com/search?q=" + encoded);
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
            }

            addressBar.clearFocus();
        } catch (Exception ignored) {
        }
    }

    private void showBrowserMenu() {
        String[] options = {
                "Bookmarks",
                "History",
                "Find on page",
                "Share page",
                "Download current page",
                "Desktop mode: " + (desktopMode ? "ON" : "OFF"),
                "Private mode: " + (privateMode ? "ON" : "OFF"),
                "Dark theme: " + (darkMode ? "ON" : "OFF"),
                "Clear browsing data",
                "Settings / About"
        };

        new AlertDialog.Builder(this)
                .setTitle("Nirva Browser Menu")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showBookmarks();
                            break;
                        case 1:
                            showHistory();
                            break;
                        case 2:
                            findOnPage();
                            break;
                        case 3:
                            shareCurrentPage();
                            break;
                        case 4:
                            if (web != null && web.getUrl() != null) {
                                downloadFile(web.getUrl(), null,
                                        null, "text/html");
                            }
                            break;
                        case 5:
                            desktopMode = !desktopMode;
                            recreateCurrentPage();
                            break;
                        case 6:
                            togglePrivateMode();
                            break;
                        case 7:
                            darkMode = !darkMode;
                            applyTheme();
                            break;
                        case 8:
                            clearBrowsingData();
                            break;
                        case 9:
                            showAbout();
                            break;
                    }
                })
                .setPositiveButton("Bookmark this page", (dialog, which) ->
                        addBookmark())
                .show();
    }

    private void showMediaOptions(String url) {
        String[] options = {
                "Download file",
                "Open media",
                "Copy link"
        };

        new AlertDialog.Builder(this)
                .setTitle("Media options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            downloadFile(url, null, null, null);
                            break;
                        case 1:
                            if (web != null) web.loadUrl(url);
                            break;
                        case 2:
                            android.content.ClipboardManager clipboard =
                                    (android.content.ClipboardManager)
                                            getSystemService(CLIPBOARD_SERVICE);

                            if (clipboard != null) {
                                clipboard.setPrimaryClip(
                                        android.content.ClipData.newPlainText(
                                                "URL", url));
                                Toast.makeText(this, "Link copied",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                })
                .show();
    }

    private void downloadFile(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Download link not found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();

            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))) {
                Toast.makeText(this,
                        "આ પ્રકારની ફાઇલ સીધી ડાઉનલોડ કરી શકાતી નથી",
                        Toast.LENGTH_LONG).show();
                return;
            }

            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            String fileName = URLUtil.guessFileName(
                    url, contentDisposition, mimeType);

            request.setTitle(fileName);
            request.setDescription("Downloading with Nirva Browser");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName);

            if (mimeType != null && !mimeType.trim().isEmpty()) {
                request.setMimeType(mimeType);
            }

            if (userAgent != null && !userAgent.trim().isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            } else if (web != null) {
                request.addRequestHeader(
                        "User-Agent", web.getSettings().getUserAgentString());
            }

            if (!privateMode) {
                String cookie = CookieManager.getInstance().getCookie(url);

                if (cookie != null && !cookie.isEmpty()) {
                    request.addRequestHeader("Cookie", cookie);
                }
            }

            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);

            DownloadManager manager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            if (manager == null) {
                Toast.makeText(this, "Download service unavailable",
                        Toast.LENGTH_LONG).show();
                return;
            }

            manager.enqueue(request);

            Toast.makeText(this,
                    "Download શરૂ થયું: " + fileName,
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Download શરૂ થઈ શક્યું નથી",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addBookmark() {
        if (web == null || web.getUrl() == null) return;

        String url = web.getUrl();

        if (!bookmarks.contains(url)) {
            bookmarks.add(url);
        }

        Toast.makeText(this, "Bookmark saved",
                Toast.LENGTH_SHORT).show();
    }

    private void showBookmarks() {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = bookmarks.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Bookmarks")
                .setItems(items, (dialog, which) -> {
                    if (web != null) web.loadUrl(items[which]);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, "No browsing history",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = history.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("History")
                .setItems(items, (dialog, which) -> {
                    if (web != null) web.loadUrl(items[which]);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void findOnPage() {
        if (web == null) return;

        EditText searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Find text");

        new AlertDialog.Builder(this)
                .setTitle("Find on page")
                .setView(searchBox)
                .setPositiveButton("Find", (dialog, which) -> {
                    String query = searchBox.getText().toString();

                    if (!query.isEmpty() && web != null) {
                        web.findAllAsync(query);

                        web.setFindListener((activeMatchOrdinal,
                                              numberOfMatches,
                                              isDoneCounting) -> {
                            statusText.setText(
                                    numberOfMatches + " matches found");
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareCurrentPage() {
        if (web == null || web.getUrl() == null) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, web.getUrl());

        startActivity(Intent.createChooser(intent, "Share page"));
    }

    private void togglePrivateMode() {
        privateMode = !privateMode;

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(!privateMode);

        if (web != null) {
            WebSettings settings = web.getSettings();
            settings.setSaveFormData(!privateMode);
            settings.setSavePassword(!privateMode);
        }

        updateTabStatus();

        Toast.makeText(this,
                privateMode
                        ? "Private mode enabled"
                        : "Private mode disabled",
                Toast.LENGTH_SHORT).show();
    }

    private void recreateCurrentPage() {
        if (web == null) return;

        String url = web.getUrl();

        WebSettings settings = web.getSettings();

        if (desktopMode) {
            settings.setUserAgentString(
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        } else {
            settings.setUserAgentString(null);
        }

        if (url != null) web.reload();
    }

    private void applyTheme() {
        int background = darkMode ? 0xFF202124 : 0xFFFFFFFF;
        int foreground = darkMode ? 0xFFFFFFFF : 0xFF202124;

        root.setBackgroundColor(background);
        toolbar.setBackgroundColor(darkMode
                ? 0xFF303134 : 0xFFF1F3F4);

        addressBar.setTextColor(foreground);
        addressBar.setHintTextColor(darkMode
                ? 0xFFBBBBBB : 0xFF666666);
        statusText.setTextColor(foreground);

        Toast.makeText(this,
                darkMode ? "Dark theme enabled" : "Light theme enabled",
                Toast.LENGTH_SHORT).show();
    }

    private void clearBrowsingData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear browsing data?")
                .setMessage("આથી WebViewનું cache, history અને cookies સાફ થશે.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    for (WebView item : tabs) {
                        item.clearCache(true);
                        item.clearHistory();
                        item.clearFormData();
                    }

                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();

                    history.clear();

                    Toast.makeText(this, "Browsing data cleared",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Nirva Browser")
                .setMessage(
                        "Nirva Browser\n\n"
                                + "Web browsing, tabs, bookmarks, history, "
                                + "page search and downloads.\n\n"
                                + "Media downloads depend on the website, "
                                + "server permissions and available links.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else if (tabs.size() > 1) {
            closeCurrentTab();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        for (WebView item : tabs) {
            try {
                item.stopLoading();
                item.destroy();
            } catch (Exception ignored) {
            }
        }

        tabs.clear();
        super.onDestroy();
    }
}
