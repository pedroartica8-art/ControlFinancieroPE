package pe.controlfinanciero.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingText = "";
    private String pendingName = "export.csv";
    private String pendingMime = "text/csv";
    private static final int SAVE_FILE = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(14,79,130));
        getWindow().setNavigationBarColor(Color.WHITE);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    public class Bridge {
        @JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("Control Financiero", text));
                Toast.makeText(MainActivity.this, "Copiado", Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void saveFile(String filename, String mime, String text) {
            pendingName = filename == null || filename.isEmpty() ? "export.txt" : filename;
            pendingMime = mime == null || mime.isEmpty() ? "text/plain" : mime;
            pendingText = text == null ? "" : text;
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(pendingMime);
                i.putExtra(Intent.EXTRA_TITLE, pendingName);
                startActivityForResult(i, SAVE_FILE);
            });
        }

        @JavascriptInterface
        public String legacyData() {
            try {
                JSONObject root = new JSONObject();
                JSONArray moves = new JSONArray();
                JSONArray pays = new JSONArray();

                File dbFile = getDatabasePath("finance.db");
                if (dbFile.exists()) {
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(
                            dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY
                    );
                    try {
                        Cursor c = db.rawQuery("SELECT date,type,cat,concept,amount,cls,nat,med,dest,month,note FROM mov ORDER BY id", null);
                        while (c.moveToNext()) {
                            JSONObject m = new JSONObject();
                            m.put("date", c.getString(0));
                            m.put("type", c.getString(1));
                            m.put("category", c.getString(2));
                            m.put("concept", c.getString(3));
                            m.put("amount", c.getDouble(4));
                            m.put("classification", c.getString(5));
                            m.put("nature", c.getString(6));
                            m.put("medium", c.getString(7));
                            m.put("destination", c.getString(8));
                            m.put("month", c.getString(9));
                            m.put("note", c.getString(10));
                            moves.put(m);
                        }
                        c.close();
                    } catch (Exception ignored) {}

                    try {
                        Cursor c = db.rawQuery("SELECT type,concept,cat,amount,day FROM pay ORDER BY id", null);
                        while (c.moveToNext()) {
                            JSONObject p = new JSONObject();
                            p.put("type", c.getString(0));
                            p.put("concept", c.getString(1));
                            p.put("category", c.getString(2));
                            p.put("amount", c.getDouble(3));
                            p.put("day", c.getInt(4));
                            p.put("medium", "Yape");
                            p.put("active", true);
                            pays.put(p);
                        }
                        c.close();
                    } catch (Exception ignored) {}
                    db.close();
                }

                SharedPreferences sp = getSharedPreferences("cfg", MODE_PRIVATE);
                JSONObject cfg = new JSONObject();
                cfg.put("expectedIncome", toDouble(sp.getString("income", "2000")));
                cfg.put("openingBalance", toDouble(sp.getString("opening", "0")));
                cfg.put("minBalance", toDouble(sp.getString("minimum", "0")));
                cfg.put("saveRate", toDouble(sp.getString("saveRate", "0.20")));

                root.put("movements", moves);
                root.put("payments", pays);
                root.put("config", cfg);
                root.put("hasData", moves.length() > 0 || pays.length() > 0 || sp.contains("income"));
                return root.toString();
            } catch (Exception e) {
                return "{\"hasData\":false}";
            }
        }
    }

    private double toDouble(String x) {
        try { return Double.parseDouble(x.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out != null) {
                    if (pendingMime.contains("csv")) {
                        out.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    }
                    out.write(pendingText.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.appBack ? window.appBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }
}
