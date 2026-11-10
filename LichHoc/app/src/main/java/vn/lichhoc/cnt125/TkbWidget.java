package vn.lichhoc.cnt125;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Widget màn hình chính, đổi màu theo buổi học tiếp theo: sáng vàng, chiều xanh, tối tím đậm. */
public class TkbWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx));
        SyncJob.schedule(ctx);
    }

    public static void refreshAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, TkbWidget.class));
        if (ids.length > 0) {
            RemoteViews v = build(ctx);
            for (int id : ids) mgr.updateAppWidget(id, v);
        }
    }

    static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        Week cur = Repo.current(ctx), nxt = Repo.next(ctx);
        LocalDateTime now = LocalDateTime.now();
        Week.Upcoming u = Week.next(now, cur, nxt);

        Ui.Buoi st = u == null ? null : Ui.buoi(u.slot.buoi);
        int bg = st == null ? R.drawable.widget_empty : st == Ui.SANG ? R.drawable.widget_sang : st == Ui.TOI ? R.drawable.widget_toi : R.drawable.widget_chieu;
        int fg = st == null ? Ui.FG : st.fg, sub = st == null ? Ui.MUTED : st.sub, accent = st == null ? Ui.BRAND : st.accent;
        v.setInt(R.id.w_root, "setBackgroundResource", bg);
        v.setTextColor(R.id.w_subject, fg);
        v.setTextColor(R.id.w_label, sub);
        v.setTextColor(R.id.w_today, sub);
        v.setTextColor(R.id.w_meta, st == Ui.TOI ? st.fg : (st == null ? Ui.BRAND : st.fg));
        v.setTextColor(R.id.w_icon, accent);
        v.setTextColor(R.id.w_duty, st == Ui.TOI ? Ui.SANG.accent : Ui.WARN);

        Week shown = cur != null ? cur : nxt;
        if (shown != null && !shown.statusLabel().isEmpty()) {
            v.setTextViewText(R.id.w_status, (shown.isFinal() ? "✓ " : "● ") + shown.statusLabel());
            v.setTextColor(R.id.w_status, shown.isFinal() ? Ui.OK : Ui.WARN);
            v.setInt(R.id.w_status, "setBackgroundResource", shown.isFinal() ? R.drawable.pill_ok : R.drawable.pill_warn);
        } else {
            v.setTextViewText(R.id.w_status, "");
            v.setInt(R.id.w_status, "setBackgroundResource", 0);
        }

        if (u == null) {
            v.setTextViewText(R.id.w_icon, "📅");
            v.setTextViewText(R.id.w_label, cur == null && nxt == null ? "CHƯA CÓ DỮ LIỆU" : "KHÔNG CÒN BUỔI HỌC");
            v.setTextViewText(R.id.w_subject, cur == null && nxt == null ? "Mở app để tải lịch" : "Nghỉ ngơi thôi");
            v.setTextViewText(R.id.w_meta, "");
        } else {
            LocalDate d = u.start.toLocalDate();
            String when = d.equals(now.toLocalDate()) ? "Hôm nay"
                    : d.equals(now.toLocalDate().plusDays(1)) ? "Ngày mai"
                    : Week.THU[u.day] + " " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
            v.setTextViewText(R.id.w_icon, st.icon);
            String label = u.ongoing ? "ĐANG HỌC · " + st.label : "BUỔI " + st.label;
            if (!u.ongoing) {
                long min = Duration.between(now, u.start).toMinutes();
                if (min < 24 * 60) label += " · còn " + (min < 60 ? min + " phút" : (min / 60) + " giờ");
            }
            v.setTextViewText(R.id.w_label, label);
            v.setTextViewText(R.id.w_subject, u.slot.mon);
            String meta = when + " · " + u.slot.gio;
            if (!u.slot.phong.isEmpty()) meta += " · " + u.slot.phong;
            if (!u.slot.gv.isEmpty()) meta += "\n" + u.slot.gv;
            v.setTextViewText(R.id.w_meta, meta);
        }

        StringBuilder today = new StringBuilder();
        if (cur != null) {
            int idx = now.getDayOfWeek().getValue() - 1;
            if (cur.monday.plusDays(idx).equals(now.toLocalDate())) {
                List<Week.Slot> slots = cur.days.get(idx);
                for (Week.Slot s : slots) {
                    if (today.length() > 0) today.append("   ");
                    today.append(Ui.buoi(s.buoi).icon).append(' ').append(s.gio).append(' ').append(s.mon);
                }
                if (slots.isEmpty()) today.append("Hôm nay không có lịch học");
            }
        }
        v.setTextViewText(R.id.w_today, today.toString());

        String duty = "";
        try {
            List<String> crew = Store.todayCrew(ctx);
            if (!crew.isEmpty()) duty = "🧹 Trực hôm nay: " + String.join(", ", crew);
        } catch (Exception ignored) { }
        v.setTextViewText(R.id.w_duty, duty);

        PendingIntent pi = PendingIntent.getActivity(ctx, 1, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.w_root, pi);
        return v;
    }
}
