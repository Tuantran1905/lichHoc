package vn.lichhoc.cnt125;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Tải file TKB, so sánh với bản đã lưu, gửi thông báo và cập nhật widget. */
public class Repo {
    public static final String CLASS_NAME = "TA.CĐ. CNT.1.25";
    static final String FILE_ID = "1v3fjuXH9cKIpYZdZ3ul7-QBSOsWSSsDM";
    static final String URL_DOWNLOAD = "https://drive.google.com/uc?export=download&id=" + FILE_ID;
    static final String CHANNEL = "tkb";
    private static final Object LOCK = new Object();

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("tkb", Context.MODE_PRIVATE); }

    public static Week current(Context c) { return Week.parse(prefs(c).getString("current", "")); }
    public static Week next(Context c) { return Week.parse(prefs(c).getString("next", "")); }
    public static String lastSync(Context c) { return prefs(c).getString("lastSync", ""); }
    public static String lastError(Context c) { return prefs(c).getString("lastError", ""); }
    public static String lastChange(Context c) { return prefs(c).getString("lastChange", ""); }

    /** Chạy trên luồng nền. Trả về null nếu thành công, ngược lại là thông báo lỗi. */
    public static String sync(Context ctx) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(ctx);
            try {
                File f = new File(ctx.getCacheDir(), "tkb.xlsx");
                try { download(f); }
                catch (Exception ex) { throw new IOException("Tải file: " + ex.getMessage(), ex); }
                LocalDate thisMon = LocalDate.now().with(DayOfWeek.MONDAY);
                List<Week> weeks;
                try {
                    weeks = TkbParser.parse(f, CLASS_NAME, java.util.Arrays.asList(thisMon, thisMon.plusDays(7)));
                    if (pick(weeks, thisMon) == null && pick(weeks, thisMon.plusDays(7)) == null)
                        weeks = TkbParser.parse(f, CLASS_NAME); // dò nhanh không thấy thì đọc toàn bộ file
                } catch (Exception ex) {
                    throw new IOException("Đọc file: " + ex.getClass().getSimpleName() + " " + ex.getMessage(), ex);
                }
                Week cur = pick(weeks, thisMon), nxt = pick(weeks, thisMon.plusDays(7));

                Week oldCur = current(ctx), oldNext = next(ctx);
                boolean firstRun = oldCur == null && oldNext == null;
                if (oldCur != null && !oldCur.monday.equals(thisMon))
                    oldCur = oldNext != null && oldNext.monday.equals(thisMon) ? oldNext : null;
                if (oldNext != null && !oldNext.monday.equals(thisMon.plusDays(7))) oldNext = null;

                if (!firstRun) {
                    checkWeek(ctx, oldCur, cur, false, 100);
                    checkWeek(ctx, oldNext, nxt, true, 200);
                }

                SharedPreferences.Editor e = p.edit();
                if (cur != null) e.putString("current", cur.serialize());
                else e.remove("current");
                if (nxt != null) e.putString("next", nxt.serialize());
                else e.remove("next");
                e.putString("lastSync", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                e.putString("lastError", "");
                e.apply();

                Store.mergeWeeks(ctx, cur, nxt);
                sundaySummary(ctx, nxt);
                return null;
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                p.edit().putString("lastError", msg).apply();
                return msg;
            } finally {
                TkbWidget.refreshAll(ctx);
            }
        }
    }

    private static Week pick(List<Week> weeks, LocalDate monday) {
        Week found = null;
        for (Week w : weeks) {
            if (!w.monday.equals(monday)) continue;
            if (found == null || w.isFinal() && !found.isFinal()) found = w;
        }
        return found;
    }

    private static void checkWeek(Context ctx, Week old, Week now, boolean isNext, int idBase) {
        if (now == null) return;
        String label = (isNext ? "tuần sau " : "tuần này ") + now.tuanNgan();
        if (old == null) {
            if (isNext) {
                String st = now.statusLabel().isEmpty() ? "" : " (" + now.statusLabel() + ")";
                notify(ctx, idBase, "📅 Đã có lịch " + label + st, now.summary());
                saveChange(ctx, "Đã có lịch " + label + st);
            }
            return;
        }
        if (!old.isFinal() && now.isFinal()) {
            notify(ctx, idBase + 1, "✅ Lịch " + label + " đã HOÀN CHỈNH", now.summary());
            saveChange(ctx, "Lịch " + label + " đã hoàn chỉnh");
        } else if (!TkbParser.norm(old.trangThai).equals(TkbParser.norm(now.trangThai)) && !now.trangThai.isEmpty()) {
            notify(ctx, idBase + 1, "ℹ️ Lịch " + label + ": " + now.statusLabel(), now.summary());
            saveChange(ctx, "Lịch " + label + " chuyển sang " + now.statusLabel());
        }
        List<String> changes = Week.diff(old, now);
        if (!changes.isEmpty()) {
            String body = String.join("\n", changes);
            notify(ctx, idBase + 2, "🔄 Lịch " + label + " thay đổi", body);
            saveChange(ctx, body);
        }
    }

    private static void sundaySummary(Context ctx, Week nxt) {
        LocalDateTime now = LocalDateTime.now();
        if (now.getDayOfWeek() != DayOfWeek.SUNDAY || now.getHour() < 19) return;
        String today = now.toLocalDate().toString();
        SharedPreferences p = prefs(ctx);
        if (today.equals(p.getString("sundaySent", ""))) return;
        if (nxt == null) notify(ctx, 300, "📅 Chưa có lịch tuần sau", "Trường chưa đăng lịch tuần sau. App sẽ báo khi có.");
        else {
            String st = nxt.statusLabel().isEmpty() ? "" : " (" + nxt.statusLabel() + ")";
            notify(ctx, 300, "📅 Lịch tuần " + nxt.tuanNgan() + st, nxt.summary());
        }
        p.edit().putString("sundaySent", today).apply();
    }

    private static void saveChange(Context ctx, String text) {
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        prefs(ctx).edit().putString("lastChange", when + " · " + text).apply();
    }

    static void download(File out) throws IOException {
        String url = URL_DOWNLOAD;
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(20000);
            c.setReadTimeout(60000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) LichHoc/1.0");
            int code = c.getResponseCode();
            if (code / 100 == 3) {
                url = new URL(new URL(url), c.getHeaderField("Location")).toString();
                c.disconnect();
                continue;
            }
            if (code != 200) throw new IOException("Không tải được file TKB (mã " + code + ")");
            File tmp = new File(out.getPath() + ".part");
            try (InputStream in = c.getInputStream(); OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            } finally {
                c.disconnect();
            }
            byte[] head = new byte[2];
            try (InputStream in = new java.io.FileInputStream(tmp)) {
                if (in.read(head) != 2 || head[0] != 'P' || head[1] != 'K')
                    throw new IOException("File TKB không còn mở công khai hoặc đã đổi định dạng");
            }
            if (!tmp.renameTo(out)) throw new IOException("Không lưu được file TKB");
            return;
        }
        throw new IOException("Không tải được file TKB (quá nhiều chuyển hướng)");
    }

    static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Lịch học", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Báo khi lịch học hoàn chỉnh hoặc thay đổi");
            nm.createNotificationChannel(ch);
        }
    }

    static void notify(Context ctx, int id, String title, String body) {
        ensureChannel(ctx);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle(title)
                .setContentText(body.split("\n")[0])
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            ctx.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) { }
    }
}
