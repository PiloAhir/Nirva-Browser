package com.nirva.browser;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.Intent;
import android.app.DownloadManager;
import android.os.Environment;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.*;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.*;

public class MainActivity extends Activity {

    final int BG = Color.rgb(244,248,250);
    final int INK = Color.rgb(25,42,52);
    final int TEAL = Color.rgb(22,125,141);
    final int MUTED = Color.rgb(98,116,126);

    final String[] engines = {
        "Google","Bing","Brave Search","DuckDuckGo"
    };

    final String[] bases = {
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q=",
        "https://search.brave.com/search?q=",
        "https://duckduckgo.com/?q="
    };

    final String HIST = "history";
    final String BOOK = "bookmarks";
    final String ENGINE = "engine";
    final String AUTO = "autoClear";

    SharedPreferences prefs;
    LinearLayout root, toolbar, body;
    EditText address;
    WebView web;

    boolean showingWeb = false;
    boolean clearing = false;
    boolean desktopMode = false;
    boolean privateMode = false;

    String currentUrl = "";

    ArrayList<String> tabUrls = new ArrayList<>();
    int activeTab = 0;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        prefs = getSharedPreferences("nirva", MODE_PRIVATE);

        setupBars();
        buildUI();
        home();
    }

    void setupBars() {
        Window w = getWindow();

        WindowCompat.setDecorFitsSystemWindows(w, false);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat c =
            new WindowInsetsControllerCompat(w, w.getDecorView());

        c.setAppearanceLightStatusBars(true);
        c.setAppearanceLightNavigationBars(true);
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setBackgroundColor(BG);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, i) -> {
            Insets x = i.getInsets(
                WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(x.left, x.top, x.right, x.bottom);
            return i;
        });

        toolbar = new LinearLayout(this);
        toolbar.setOrientation(1);
        toolbar.setPadding(dp(15), dp(8), dp(15), dp(8));
        toolbar.setBackgroundColor(BG);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = txt("⬡",31,TEAL,true);
        logo.setGravity(Gravity.CENTER);

        brand.addView(
            logo,
            new LinearLayout.LayoutParams(dp(42),dp(42))
        );

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(1);
        words.addView(txt("NIRVA",19,INK,true));
        words.addView(txt("Browse with more control",11,MUTED,false));

        LinearLayout.LayoutParams wp =
            new LinearLayout.LayoutParams(0,-2,1);
        wp.leftMargin = dp(8);
        brand.addView(words,wp);

        Button settings = btn("⚙");
        settings.setOnClickListener(v -> settings());

        brand.addView(
            settings,
            new LinearLayout.LayoutParams(dp(45),dp(42))
        );

        toolbar.addView(brand);

        LinearLayout search = new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(8),dp(2),dp(3),dp(2));
        search.setBackground(
            round(Color.WHITE,dp(15),0xFFDCE7EB)
        );

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(15);
        address.setTextColor(INK);
        address.setHintTextColor(MUTED);
        address.setHint("Search or enter website");
        address.setPadding(dp(8),dp(8),dp(4),dp(8));
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);

        address.setOnEditorActionListener((v,a,e) -> {
            if (a == EditorInfo.IME_ACTION_GO
                    || a == EditorInfo.IME_ACTION_DONE) {
                navigate();
                return true;
            }
            return false;
        });

        search.addView(
            address,
            new LinearLayout.LayoutParams(0,-2,1)
        );

        Button go = btn("Go");
        go.setOnClickListener(v -> navigate());

        search.addView(
            go,
            new LinearLayout.LayoutParams(dp(52),dp(42))
        );

        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin = dp(8);
        toolbar.addView(search,sp);

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER);
        tools.setPadding(dp(5),dp(2),dp(5),dp(5));

        String[] toolNames = {
            "↻","Tabs","Find","Desktop","Share","Private"
        };

        for (int i = 0; i < toolNames.length; i++) {
            final int n = i;

            Button tb = btn(toolNames[i]);
            tb.setTextSize(12);
            tb.setOnClickListener(v -> toolAction(n));

            LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(0,dp(38),1);
            tp.setMargins(dp(2),0,dp(2),0);
            tools.addView(tb,tp);
        }

        toolbar.addView(tools);

        body = new LinearLayout(this);
        body.setOrientation(1);

        root.addView(
            body,
            new LinearLayout.LayoutParams(-1,0,1)
        );

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6),dp(5),dp(6),dp(4));
        nav.setBackgroundColor(Color.WHITE);

        String[] labels = {"‹","›","⌂","☆","☷"};
        String[] descriptions = {
            "Back","Forward","Home","Bookmark","History"
        };

        for (int i = 0; i < labels.length; i++) {
            final int n = i;

            Button b = btn(labels[i]);
            b.setTextSize(i == 2 ? 22 : 20);
            b.setContentDescription(descriptions[i]);
            b.setOnClickListener(v -> navAction(n));

            LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0,dp(43),1);
            p.setMargins(dp(3),0,dp(3),0);
            nav.addView(b,p);
        }

        root.addView(
            nav,
            new LinearLayout.LayoutParams(-1,-2)
        );

        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    void home() {
        showingWeb = false;

        if (!tabUrls.isEmpty() && activeTab < tabUrls.size()) {
            tabUrls.set(activeTab,"");
        }

        body.removeAllViews();
        detachWeb();

        ScrollView sc = new ScrollView(this);

        LinearLayout h = new LinearLayout(this);
        h.setOrientation(1);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(dp(22),dp(22),dp(22),dp(20));

        TextView logo = txt("⬡",64,TEAL,true);
        logo.setGravity(Gravity.CENTER);

        h.addView(
            logo,
            new LinearLayout.LayoutParams(-1,dp(82))
        );

        TextView title = txt("Nirva Browser",27,INK,true);
        title.setGravity(Gravity.CENTER);
        h.addView(title);

        TextView sub = txt(
            "તમારી વેબ. તમારી પસંદગી. વધુ નિયંત્રણ.",
            14,MUTED,false
        );
        sub.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);
        p.topMargin = dp(6);
        h.addView(sub,p);

        Button choose = btn("Search with " + engine() + "  ▾");
        choose.setOnClickListener(v -> chooseEngine());

        LinearLayout.LayoutParams cp =
            new LinearLayout.LayoutParams(-2,dp(46));
        cp.topMargin = dp(18);
        h.addView(choose,cp);

        TextView privacy = txt(
            "PRIVACY-FIRST CONTROLS",11,MUTED,true
        );
        privacy.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams pp =
            new LinearLayout.LayoutParams(-1,-2);
        pp.topMargin = dp(25);
        pp.bottomMargin = dp(10);
        h.addView(privacy,pp);

        h.addView(card(
            "🔒","Privacy controls",
            "Tracker blocking and data-clearing options",
            () -> settings()
        ));

        h.addView(card(
            "☆","Your bookmarks",
            "Open pages you saved",
            () -> showList(BOOK,"Your bookmarks")
        ));

        h.addView(card(
            "◷","Recent history",
            "Review saved visits",
            () -> showList(HIST,"Recent history")
        ));

        TextView note = txt(
            "Early version: privacy protection is not absolute.",
            11,MUTED,false
        );
        note.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams np =
            new LinearLayout.LayoutParams(-1,-2);
        np.topMargin = dp(15);
        h.addView(note,np);

        sc.addView(h);
        body.addView(sc,new LinearLayout.LayoutParams(-1,-1));

        address.setText("");
        address.setHint(
            "Search with " + engine() + " or enter website"
        );
    }

    View card(String icon,String title,String desc,Runnable run) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(12),dp(10),dp(10),dp(10));
        c.setBackground(
            round(Color.WHITE,dp(15),0xFFE1EAED)
        );

        TextView ic = txt(icon,23,TEAL,true);
        ic.setGravity(Gravity.CENTER);

        c.addView(
            ic,
            new LinearLayout.LayoutParams(dp(40),dp(42))
        );

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(1);
        texts.addView(txt(title,15,INK,true));
        texts.addView(txt(desc,12,MUTED,false));

        LinearLayout.LayoutParams tp =
            new LinearLayout.LayoutParams(0,-2,1);
        tp.leftMargin = dp(8);
        c.addView(texts,tp);

        c.setOnClickListener(v -> run.run());

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin = dp(8);
        c.setLayoutParams(p);

        return c;
    }

    void navigate() {
        String in = address.getText().toString().trim();
        if (in.isEmpty()) return;

        ((InputMethodManager)getSystemService(
            INPUT_METHOD_SERVICE
        )).hideSoftInputFromWindow(address.getWindowToken(),0);

        String low = in.toLowerCase(Locale.ROOT);
        String url;

        if (low.startsWith("https://") || low.startsWith("http://")) {
            url = in;
        } else if (in.contains(" ") || !in.contains(".")) {
            try {
                url = bases[engineIndex()]
                    + URLEncoder.encode(in,"UTF-8");
            } catch (Exception e) {
                url = bases[engineIndex()]
                    + in.replace(" ","+");
            }
        } else {
            url = "https://" + in;
        }

        open(url);
    }

    void open(String url) {
        if (web == null) createWeb();

        if (tabUrls.isEmpty()) {
            tabUrls.add(url);
            activeTab = 0;
        } else if (activeTab < tabUrls.size()) {
            tabUrls.set(activeTab,url);
        }

        showingWeb = true;
        currentUrl = url;

        detachWeb();
        body.removeAllViews();
        body.addView(web,new LinearLayout.LayoutParams(-1,-1));

        address.setText(url);
        web.loadUrl(url);

        if (!privateMode) {
            save(
                HIST,
                System.currentTimeMillis() + "|"
                    + url.replace("|","%7C"),
                300
            );
        }
    }

    void detachWeb() {
        if (web != null && web.getParent() != null) {
            ((ViewGroup)web.getParent()).removeView(web);
        }
    }

    void createWeb() {
        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(!privateMode);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(
            privateMode
                ? WebSettings.LOAD_NO_CACHE
                : WebSettings.LOAD_DEFAULT
        );
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );
        s.setSafeBrowsingEnabled(true);

        CookieManager.getInstance().setAcceptCookie(!privateMode);
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            web,false
        );

        web.setWebChromeClient(new WebChromeClient());

        if (desktopMode) {
            s.setUserAgentString(
                "Mozilla/5.0 (X11; Linux x86_64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/131.0.0.0 Safari/537.36"
            );
        }

        web.setDownloadListener((url,ua,cd,mime,len) -> {
            downloadFile(url,ua,cd,mime);
        });

        // Long-press menu for images, media and direct file links.
        web.setOnLongClickListener(v -> {
            WebView.HitTestResult result = web.getHitTestResult();

            if (result == null) return false;

            int type = result.getType();
            String url = result.getExtra();

            boolean isMedia =
                type == WebView.HitTestResult.SRC_IMAGE_TYPE
                || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                || type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                || type == WebView.HitTestResult.SRC_VIDEO_TYPE
                || type == WebView.HitTestResult.SRC_AUDIO_TYPE;

            if (!isMedia || url == null || url.trim().isEmpty()) {
                return false;
            }

            final String fileUrl = url;

            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Media / File options")
                .setItems(
                    new String[] {
                        "Download file",
                        "Open media",
                        "Cancel"
                    },
                    (dialog,which) -> {
                        if (which == 0) {
                            downloadFile(fileUrl,null,null,null);
                        } else if (which == 1) {
                            open(fileUrl);
                        }
                    }
                )
                .show();

            return true;
        });

        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView v,WebResourceRequest r) {

                String scheme = r.getUrl().getScheme();

                if ("https".equals(scheme) || "http".equals(scheme)) {
                    return false;
                }

                Toast.makeText(
                    MainActivity.this,
                    "આ લિંકનો પ્રકાર સપોર્ટેડ નથી",
                    Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView v,WebResourceRequest r) {

                if (tracker(r.getUrl().getHost())) {
                    return new WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        new ByteArrayInputStream(new byte[0])
                    );
                }

                return super.shouldInterceptRequest(v,r);
            }

            @Override
            public void onPageFinished(WebView v,String u) {
                currentUrl = u;
                address.setText(u);

                if (!privateMode && activeTab < tabUrls.size()) {
                    tabUrls.set(activeTab,u);
                }
            }
        });
    }

    // Download HTTP/HTTPS files using Android DownloadManager.
    void downloadFile(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        if (url == null || url.trim().isEmpty()) {
            toast("ફાઇલની લિંક મળી નથી");
            return;
        }

        if (!url.startsWith("https://")
                && !url.startsWith("http://")) {
            toast("આ પ્રકારની ફાઇલ સીધી ડાઉનલોડ કરી શકાતી નથી");
            return;
        }

        try {
            DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(url));

            String fileName = URLUtil.guessFileName(
                url,contentDisposition,mimeType
            );

            request.setTitle(fileName);
            request.setDescription("Nirva Browser download");

            request.setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            if (mimeType != null && !mimeType.trim().isEmpty()) {
                request.setMimeType(mimeType);
            }

            String cookies =
                CookieManager.getInstance().getCookie(url);

            if (cookies != null && !privateMode) {
                request.addRequestHeader("Cookie",cookies);
            }

            if (userAgent != null && !userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent",userAgent);
            }

            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,fileName
            );

            DownloadManager manager = (DownloadManager)
                getSystemService(DOWNLOAD_SERVICE);

            if (manager == null) {
                toast("Download Manager ઉપલબ્ધ નથી");
                return;
            }

            manager.enqueue(request);
            toast("ડાઉનલોડ શરૂ થયું: " + fileName);

        } catch (Exception e) {
            toast("ફાઇલ ડાઉનલોડ થઈ શકી નથી");
        }
    }

    boolean tracker(String host) {
        if (host == null) return false;

        String[] blocked = {
            "doubleclick.net",
            "googlesyndication.com",
            "google-analytics.com",
            "googletagmanager.com",
            "connect.facebook.net",
            "scorecardresearch.com",
            "hotjar.com",
            "segment.io",
            "ads-twitter.com",
            "app-measurement.com"
        };

        for (String d : blocked) {
            if (host.equalsIgnoreCase(d)
                    || host.toLowerCase(Locale.ROOT).endsWith("." + d)) {
                return true;
            }
        }

        return false;
    }

    void toolAction(int n) {
        if (n == 0) {
            if (web != null && showingWeb) web.reload();
            else home();

        } else if (n == 1) {
            ArrayList<String> choices = new ArrayList<>();

            for (int i = 0; i < tabUrls.size(); i++) {
                choices.add(
                    (i == activeTab ? "● " : "")
                    + "Tab " + (i + 1) + " — " + tabUrls.get(i)
                );
            }

            if (choices.isEmpty()) choices.add("Home");

            String[] items = choices.toArray(new String[0]);

            new AlertDialog.Builder(this)
                .setTitle("Tabs (" + Math.max(1,tabUrls.size()) + ")")
                .setItems(items,(d,which) -> {
                    if (!tabUrls.isEmpty()) {
                        activeTab = which;

                        if (tabUrls.get(which) == null
                                || tabUrls.get(which).isEmpty()) {
                            home();
                        } else {
                            open(tabUrls.get(which));
                        }
                    } else {
                        home();
                    }
                })
                .setPositiveButton("+ New tab",(d,w) -> {
                    tabUrls.add("");
                    activeTab = tabUrls.size() - 1;
                    home();
                })
                .setNeutralButton("Close current",(d,w) -> {
                    if (!tabUrls.isEmpty()) {
                        tabUrls.remove(
                            Math.min(activeTab,tabUrls.size()-1)
                        );

                        if (tabUrls.isEmpty()) {
                            activeTab = 0;
                            home();
                        } else {
                            activeTab = Math.max(
                                0,Math.min(activeTab,tabUrls.size()-1)
                            );

                            if (tabUrls.get(activeTab) == null
                                    || tabUrls.get(activeTab).isEmpty()) {
                                home();
                            } else {
                                open(tabUrls.get(activeTab));
                            }
                        }
                    } else {
                        home();
                    }
                })
                .setNegativeButton("Done",null)
                .show();

        } else if (n == 2) {
            if (web == null || !showingWeb) {
                toast("પહેલાં વેબપેજ ખોલો");
                return;
            }

            EditText q = new EditText(this);
            q.setSingleLine(true);
            q.setHint("Find text on page");

            new AlertDialog.Builder(this)
                .setTitle("Find in page")
                .setView(q)
                .setPositiveButton("Find",(d,w) -> {
                    String query = q.getText().toString();

                    if (query.trim().isEmpty()) {
                        toast("શોધવા માટે શબ્દ લખો");
                        return;
                    }

                    web.setFindListener(
                        (activeMatchOrdinal,numberOfMatches,isDoneCounting) -> {
                            if (isDoneCounting && numberOfMatches == 0) {
                                toast("શબ્દ મળ્યો નથી");
                            }
                        }
                    );

                    web.findAllAsync(query);
                })
                .setNeutralButton("Next",(d,w) -> {
                    if (web != null) web.findNext(true);
                })
                .setNegativeButton("Close",(d,w) -> {
                    if (web != null) web.clearMatches();
                })
                .show();

        } else if (n == 3) {
            desktopMode = !desktopMode;

            if (web != null) {
                String u = currentUrl;
                detachWeb();
                web = null;
                if (!u.isEmpty()) open(u);
            }

            toast("Desktop site " + (desktopMode ? "ON" : "OFF"));

        } else if (n == 4) {
            if (currentUrl.isEmpty()) {
                toast("પહેલાં વેબસાઇટ ખોલો");
                return;
            }

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,currentUrl);

            startActivity(
                Intent.createChooser(share,"Share page")
            );

        } else if (n == 5) {
            privateMode = !privateMode;

            if (privateMode) {
                CookieManager.getInstance().removeAllCookies(v -> {});

                if (web != null) {
                    web.clearHistory();
                    web.clearCache(true);
                }
            } else {
                CookieManager.getInstance().setAcceptCookie(true);
            }

            if (web != null) {
                String u = currentUrl;
                detachWeb();
                web = null;

                if (!u.isEmpty()) open(u);
                else home();
            }

            toast("Private mode " + (privateMode ? "ON" : "OFF"));
        }
    }

    void navAction(int n) {
        switch (n) {
            case 0:
                if (web != null && web.canGoBack()) web.goBack();
                else toast("પાછળ પેજ નથી");
                break;

            case 1:
                if (web != null && web.canGoForward()) web.goForward();
                else toast("આગળ પેજ નથી");
                break;

            case 2:
                home();
                break;

            case 3:
                bookmark();
                break;

            case 4:
                showList(HIST,"Recent history");
                break;
        }
    }

    void chooseEngine() {
        new AlertDialog.Builder(this)
            .setTitle("Choose search engine")
            .setSingleChoiceItems(engines,engineIndex(),(d,n) -> {
                prefs.edit().putString(ENGINE,engines[n]).apply();
                d.dismiss();

                if (showingWeb) {
                    address.setHint(
                        "Search with " + engine() + " or enter website"
                    );
                } else {
                    home();
                }

                toast("Search engine: " + engine());
            })
            .setNegativeButton("Cancel",null)
            .show();
    }

    String engine() {
        return prefs.getString(ENGINE,"Google");
    }

    int engineIndex() {
        for (int i = 0; i < engines.length; i++) {
            if (engines[i].equals(engine())) return i;
        }
        return 0;
    }

    void settings() {
        String[] opts = {
            "Search engine: " + engine(),
            "Auto-clear when app backgrounds: "
                + (prefs.getBoolean(AUTO,true) ? "ON" : "OFF"),
            "Clear browsing data",
            "Privacy information",
            "Open downloads",
            "Desktop site: " + (desktopMode ? "ON" : "OFF"),
            "Private mode: " + (privateMode ? "ON" : "OFF")
        };

        new AlertDialog.Builder(this)
            .setTitle("Nirva settings")
            .setItems(opts,(d,n) -> {
                if (n == 0) {
                    chooseEngine();

                } else if (n == 1) {
                    boolean next = !prefs.getBoolean(AUTO,true);
                    prefs.edit().putBoolean(AUTO,next).apply();
                    toast("Auto-clear " + (next ? "ON" : "OFF"));
                    settings();

                } else if (n == 2) {
                    clearOptions();

                } else if (n == 4) {
                    Intent i = new Intent(
                        DownloadManager.ACTION_VIEW_DOWNLOADS
                    );

                    try {
                        startActivity(i);
                    } catch (Exception ex) {
                        toast("Downloads ખોલી શકાતું નથી");
                    }

                } else if (n == 5) {
                    toolAction(3);

                } else if (n == 6) {
                    toolAction(5);

                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Privacy information")
                        .setMessage(
                            "Nirva uses Android WebView and a limited "
                            + "tracker-host blocklist. It includes bookmarks, "
                            + "history, downloads, desktop mode, share, "
                            + "find-in-page and URL-based tab switching. "
                            + "Private mode is best-effort, not equivalent "
                            + "to Chrome Incognito. This app is not "
                            + "security-audited and cannot guarantee all "
                            + "tracking is blocked."
                        )
                        .setPositiveButton("OK",null)
                        .show();
                }
            })
            .setNegativeButton("Close",null)
            .show();
    }

    void clearOptions() {
        String[] opts = {
            "Last 24 hours (history only)",
            "Last 1 week (history only)",
            "Last 1 month (history only)",
            "Last 1 year (history only)",
            "Clear ALL browsing data"
        };

        new AlertDialog.Builder(this)
            .setTitle("Clear browsing data")
            .setMessage(
                "Time ranges clear saved visit history only. "
                + "Cookies, cache and site storage are cleared "
                + "together in Clear ALL."
            )
            .setItems(opts,(d,n) -> {
                if (n == 4) {
                    new AlertDialog.Builder(this)
                        .setTitle("Clear all data?")
                        .setMessage(
                            "This clears cookies, cache, WebView history, "
                            + "form data, site storage, saved history "
                            + "and bookmarks."
                        )
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton(
                            "Clear all",(x,y) -> clearAll(true)
                        )
                        .show();
                } else {
                    long days = n == 0 ? 1
                        : n == 1 ? 7
                        : n == 2 ? 30 : 365;

                    clearHistory(
                        System.currentTimeMillis() - days * 86400000L
                    );

                    toast("Selected history cleared");
                }
            })
            .setNegativeButton("Cancel",null)
            .show();
    }

    void clearAll(boolean notify) {
        if (clearing) return;
        clearing = true;

        try {
            if (web != null) {
                web.stopLoading();
                web.clearHistory();
                web.clearFormData();
                web.clearCache(true);
                web.clearSslPreferences();
            }

            CookieManager.getInstance().removeAllCookies(v -> {});
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();

            prefs.edit().remove(HIST).remove(BOOK).apply();

            if (notify) toast("All browsing data cleared");

        } catch (Exception e) {
            if (notify) toast("કેટલાક ડેટા સાફ ન થઈ શક્યા");
        } finally {
            clearing = false;
        }
    }

    void clearHistory(long before) {
        List<String> keep = new ArrayList<>();

        for (String row : lines(HIST)) {
            String[] p = row.split("\\|",2);

            try {
                if (p.length == 2 && Long.parseLong(p[0]) < before) {
                    keep.add(row);
                }
            } catch (Exception ignored) {
            }
        }

        prefs.edit().putString(HIST,String.join("\n",keep)).apply();
    }

    void bookmark() {
        if (!showingWeb || currentUrl.isEmpty()) {
            toast("પહેલાં વેબસાઇટ ખોલો");
            return;
        }

        List<String> b = lines(BOOK);

        if (!b.contains(currentUrl)) b.add(currentUrl);

        prefs.edit().putString(BOOK,String.join("\n",b)).apply();
        toast("Bookmark saved");
    }

    void save(String key,String row,int max) {
        List<String> a = lines(key);
        a.add(row);

        while (a.size() > max) a.remove(0);

        prefs.edit().putString(key,String.join("\n",a)).apply();
    }

    List<String> lines(String key) {
        String s = prefs.getString(key,"");

        return s.isEmpty()
            ? new ArrayList<>()
            : new ArrayList<>(Arrays.asList(s.split("\n")));
    }

    void showList(String key,String title) {
        List<String> a = lines(key);
        List<String> urls = new ArrayList<>();

        if (key.equals(HIST)) {
            for (String row : a) {
                String[] p = row.split("\\|",2);
                if (p.length == 2) urls.add(p[1]);
            }

            Collections.reverse(urls);
        } else {
            urls = a;
        }

        if (urls.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Nothing saved yet.")
                .setPositiveButton("OK",null)
                .show();
            return;
        }

        String[] arr = urls.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(arr,(d,n) -> open(arr[n]))
            .setNeutralButton("Clear list",(d,n) -> {
                prefs.edit().remove(key).apply();
                toast("List cleared");
            })
            .setNegativeButton("Close",null)
            .show();
    }

    TextView txt(String s,int size,int color,boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        }

        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(TEAL);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(4),0,dp(4),0);
        b.setBackground(round(Color.WHITE,dp(13),0xFFE1EAED));
        return b;
    }

    GradientDrawable round(int color,int radius,int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);

        if (stroke != 0) d.setStroke(dp(1),stroke);

        return d;
    }

    int dp(int n) {
        return (int)(
            n * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    void toast(String s) {
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (showingWeb && web != null && web.canGoBack()) {
            web.goBack();
        } else {
            home();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (prefs != null && prefs.getBoolean(AUTO,true) && !clearing) {
            clearAll(false);

            if (web != null) web.loadUrl("about:blank");

            showingWeb = false;
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            try {
                if (prefs != null && prefs.getBoolean(AUTO,true)) {
                    clearAll(false);
                }

                web.destroy();
            } catch (Exception ignored) {
            }

            web = null;
        }

        super.onDestroy();
    }
}
