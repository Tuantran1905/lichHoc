package vn.lichhoc.cnt125;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * In-app updates from the home server (status.binmall.shop). The server pulls each signed
 * GitHub Actions build; this checks /apps/<id>/version on launch, downloads the APK, verifies
 * its SHA-256 and hands it to Android's installer (the user taps "Install").
 * The update token baked in at build time can only download updates, nothing else.
 */
public final class AppUpdater {
    public static final String ACTION_INSTALL_STATUS = "vn.lichhoc.cnt125.INSTALL_STATUS";
    private static File pending;
    private static boolean checkedThisRun;

    private AppUpdater() {}

    private static boolean configured() {
        return !BuildConfig.UPDATE_TOKEN.isEmpty() && !BuildConfig.UPDATE_URL.isEmpty();
    }

    private static HttpURLConnection open(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BuildConfig.UPDATE_URL + "/apps/" + BuildConfig.UPDATE_APP_ID + path).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Authorization", "Bearer " + BuildConfig.UPDATE_TOKEN);
        return c;
    }

    private static long installedCode(Activity a) throws Exception {
        PackageInfo pi = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
    }

    /** Call from onCreate. manual=true also reports "already up to date" / errors. */
    public static void check(Activity a, boolean manual) {
        if (!configured() || (checkedThisRun && !manual)) return;
        checkedThisRun = true;
        new Thread(() -> {
            try {
                HttpURLConnection c = open("/version");
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                String body;
                try (InputStream in = c.getInputStream()) { body = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
                JSONObject j = new JSONObject(body);
                long code = j.getLong("versionCode");
                String name = j.optString("versionName");
                String sha = j.optString("sha256");
                long size = j.optLong("size");
                if (code <= installedCode(a)) {
                    if (manual) a.runOnUiThread(() -> toast(a, "Bạn đang dùng bản mới nhất"));
                    return;
                }
                a.runOnUiThread(() -> {
                    if (a.isFinishing()) return;
                    new AlertDialog.Builder(a)
                            .setTitle("Có bản cập nhật")
                            .setMessage("Phiên bản mới: " + name + "\nTải về và cài đặt ngay?")
                            .setPositiveButton("Cập nhật", (d, w) -> download(a, sha, size))
                            .setNegativeButton("Để sau", null)
                            .show();
                });
            } catch (Exception e) {
                if (manual) a.runOnUiThread(() -> toast(a, "Không kiểm tra được cập nhật: " + e.getMessage()));
            }
        }).start();
    }

    private static void download(Activity a, String sha, long size) {
        toast(a, "Đang tải bản cập nhật…");
        new Thread(() -> {
            try {
                File dir = new File(a.getCacheDir(), "updates");
                dir.mkdirs();
                File f = new File(dir, "update.apk");
                HttpURLConnection c = open("/latest.apk");
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); md.update(buf, 0, n); }
                }
                StringBuilder hex = new StringBuilder();
                for (byte x : md.digest()) hex.append(String.format("%02x", x));
                if (!sha.isEmpty() && !sha.equalsIgnoreCase(hex.toString())) {
                    f.delete();
                    throw new Exception("file tải về bị lỗi, hãy thử lại");
                }
                a.runOnUiThread(() -> installOrAskPermission(a, f));
            } catch (Exception e) {
                a.runOnUiThread(() -> toast(a, "Tải bản cập nhật thất bại: " + e.getMessage()));
            }
        }).start();
    }

    private static void installOrAskPermission(Activity a, File apk) {
        if (a.getPackageManager().canRequestPackageInstalls()) { install(a, apk); return; }
        pending = apk;
        new AlertDialog.Builder(a)
                .setTitle("Cần cấp quyền một lần")
                .setMessage("Hãy bật \"Cho phép từ nguồn này\", rồi quay lại app để cài đặt.")
                .setPositiveButton("Mở cài đặt", (d, w) -> a.startActivity(new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()))))
                .setNegativeButton("Huỷ", (d, w) -> pending = null)
                .show();
    }

    /** Call from onResume: continues after the user granted "install unknown apps". */
    public static void onResume(Activity a) {
        if (pending != null && a.getPackageManager().canRequestPackageInstalls()) {
            File f = pending;
            pending = null;
            install(a, f);
        }
    }

    private static void install(Activity a, File apk) {
        new Thread(() -> {
            try {
                PackageInstaller pi = a.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(a.getPackageName());
                if (Build.VERSION.SDK_INT >= 31)
                    params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                int id = pi.createSession(params);
                try (PackageInstaller.Session s = pi.openSession(id)) {
                    try (OutputStream out = s.openWrite("base.apk", 0, apk.length());
                         InputStream in = new java.io.FileInputStream(apk)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        s.fsync(out);
                    }
                    Intent cb = new Intent(a, a.getClass()).setAction(ACTION_INSTALL_STATUS);
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                    s.commit(PendingIntent.getActivity(a, id, cb, flags).getIntentSender());
                }
            } catch (Exception e) {
                a.runOnUiThread(() -> toast(a, "Không cài được: " + e.getMessage()));
            }
        }).start();
    }

    /** Call from onCreate and onNewIntent with the activity's intent. */
    @SuppressWarnings("deprecation")
    public static void handleIntent(Activity a, Intent i) {
        if (i == null || !ACTION_INSTALL_STATUS.equals(i.getAction())) return;
        int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) a.startActivity(confirm);
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            toast(a, "Đã cập nhật xong");
        } else {
            String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            if (msg != null && !msg.toLowerCase().contains("abort")) toast(a, "Cài đặt thất bại: " + msg);
        }
        a.setIntent(new Intent(a.getIntent()).setAction(Intent.ACTION_MAIN));
    }

    private static void toast(Activity a, String m) { Toast.makeText(a, m, Toast.LENGTH_LONG).show(); }
}
