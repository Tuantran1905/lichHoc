package vn.lichhoc.cnt125;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Ba tab: Lịch học, Trực lớp (điểm danh + người trực), Lớp (danh sách, thống kê, quy định). */
public class MainActivity extends Activity {
    private LinearLayout root, tabBar;
    private ScrollView sv;
    private String tab = "lich";
    private Button refresh;
    private boolean syncing;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Repo.ensureChannel(this);
        SyncJob.schedule(this);
        getWindow().setStatusBarColor(Ui.BRAND);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        if (b != null) tab = b.getString("tab", "lich");

        LinearLayout outer = Ui.vbox(this);
        outer.setBackgroundColor(Ui.BG);
        outer.setFitsSystemWindows(true);
        outer.addView(Ui.appBar(this, "Lịch học & Trực lớp", "Lớp trưởng · TA.CĐ.CNT.1.25"));
        tabBar = Ui.hbox(this);
        tabBar.setBackgroundColor(Ui.CARD);
        tabBar.setElevation(Ui.dp(this, 2));
        outer.addView(tabBar);

        sv = new ScrollView(this);
        root = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p * 2);
        sv.addView(root);
        outer.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(outer);
        render();
        if (Repo.current(this) == null && Repo.next(this) == null) syncNow();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("tab", tab); }

    @Override
    protected void onResume() { super.onResume(); render(); }

    private void syncNow() {
        if (syncing) return;
        syncing = true;
        render();
        new Thread(() -> {
            String err = Repo.sync(getApplicationContext());
            runOnUiThread(() -> {
                syncing = false;
                render();
                Toast.makeText(this, err == null ? "Đã cập nhật lịch" : "Lỗi: " + err, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void render() {
        renderTabs();
        root.removeAllViews();
        if (tab.equals("truc")) renderTruc();
        else if (tab.equals("lop")) renderLop();
        else renderLich();
    }

    private void renderTabs() {
        tabBar.removeAllViews();
        String[][] items = {{"lich", "Lịch học"}, {"truc", "Trực lớp"}, {"lop", "Lớp"}};
        for (String[] it : items) {
            boolean on = it[0].equals(tab);
            LinearLayout cell = Ui.vbox(this);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView t = Ui.text(this, it[1], 15, on ? Ui.BRAND : Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 11));
            cell.addView(t, new LinearLayout.LayoutParams(-1, -2));
            TextView line = new TextView(this);
            line.setBackgroundColor(on ? Ui.BRAND : Color.TRANSPARENT);
            cell.addView(line, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 3)));
            cell.setOnClickListener(v -> { tab = it[0]; render(); sv.scrollTo(0, 0); });
            tabBar.addView(cell, Ui.weight(1));
        }
    }

    // ===================== LỊCH HỌC =====================
    private void renderLich() {
        Week cur = Repo.current(this), nxt = Repo.next(this);
        LocalDateTime now = LocalDateTime.now();

        Week.Upcoming u = Week.next(now, cur, nxt);
        if (u != null) root.addView(hero(u, now));

        String change = Repo.lastChange(this);
        if (!change.isEmpty()) {
            LinearLayout n = Ui.vbox(this);
            n.setBackground(Ui.round(this, Ui.WARN_SOFT, 14));
            n.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            n.addView(Ui.label(this, "Thay đổi mới", Ui.WARN));
            n.addView(Ui.text(this, change, 14, Ui.FG, false), Ui.lp(this, 4));
            root.addView(n, Ui.lp(this, 14));
        }

        if (cur != null) addWeek("Tuần này", cur);
        if (nxt != null) addWeek("Tuần sau", nxt);
        if (cur == null && nxt == null) {
            LinearLayout e = Ui.card(this, false);
            e.addView(Ui.text(this, "Chưa có lịch", 16, Ui.FG, true));
            e.addView(Ui.text(this, "Bấm Cập nhật ngay để tải thời khóa biểu từ file của khoa.", 14, Ui.MUTED, false), Ui.lp(this, 4));
            root.addView(e, Ui.lp(this, 14));
        }

        refresh = Ui.button(this, syncing ? "Đang tải lịch…" : "Cập nhật ngay", true);
        refresh.setEnabled(!syncing);
        refresh.setOnClickListener(v -> syncNow());
        root.addView(refresh, Ui.lp(this, 20));
        String info = "Tự kiểm tra khoảng 2 giờ một lần khi có mạng.";
        if (!Repo.lastSync(this).isEmpty()) info = "Kiểm tra lần cuối " + Repo.lastSync(this) + " · " + info;
        if (!Repo.lastError(this).isEmpty()) info = "Lỗi lần trước: " + Repo.lastError(this) + "\n" + info;
        TextView t = Ui.text(this, info, 12, Ui.MUTED, false);
        t.setGravity(Gravity.CENTER);
        root.addView(t, Ui.lp(this, 8));
    }

    /** Thẻ lớn "buổi tiếp theo", tô theo buổi; buổi tối nền tím đậm. */
    private LinearLayout hero(Week.Upcoming u, LocalDateTime now) {
        Ui.Buoi st = Ui.buoi(u.slot.buoi);
        LinearLayout h = Ui.vbox(this);
        h.setBackground(Ui.round(this, st.bg, 20));
        h.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 18));
        h.setElevation(Ui.dp(this, 2));

        LinearLayout top = Ui.hbox(this);
        top.addView(Ui.label(this, u.ongoing ? "Đang học" : "Buổi học tiếp theo", st.sub), Ui.weight(1));
        top.addView(Ui.pill(this, st.icon + "  " + st.label, st == Ui.TOI ? Ui.TOI.bg : Color.WHITE, st.accent));
        h.addView(top);

        h.addView(Ui.text(this, u.slot.mon, 22, st.fg, true), Ui.lp(this, 8));
        LocalDate d = u.start.toLocalDate();
        String when = d.equals(now.toLocalDate()) ? "Hôm nay" : d.equals(now.toLocalDate().plusDays(1)) ? "Ngày mai" : Ui.day(d);
        h.addView(Ui.text(this, when + " · " + u.slot.gio + (u.slot.phong.isEmpty() ? "" : " · Phòng " + u.slot.phong), 15, st.fg, false), Ui.lp(this, 4));
        if (!u.slot.gv.isEmpty()) h.addView(Ui.text(this, u.slot.gv, 14, st.sub, false), Ui.lp(this, 2));
        if (!u.ongoing) {
            long min = Duration.between(now, u.start).toMinutes();
            String left = min < 60 ? "Còn " + min + " phút" : min < 24 * 60 ? "Còn " + (min / 60) + " giờ " + (min % 60) + " phút" : "Còn " + (min / (24 * 60)) + " ngày";
            h.addView(Ui.text(this, left, 13, st.accent, true), Ui.lp(this, 10));
        }
        return h;
    }

    private void addWeek(String title, Week w) {
        LinearLayout head = Ui.hbox(this);
        LinearLayout tt = Ui.vbox(this);
        tt.addView(Ui.label(this, title, Ui.MUTED));
        tt.addView(Ui.text(this, w.tuan(), 18, Ui.FG, true));
        head.addView(tt, Ui.weight(1));
        if (!w.statusLabel().isEmpty())
            head.addView(w.isFinal() ? Ui.pill(this, "✓ " + w.statusLabel(), Ui.OK, Ui.OK_SOFT) : Ui.pill(this, "● " + w.statusLabel(), Ui.WARN, Ui.WARN_SOFT));
        root.addView(head, Ui.lp(this, 24));

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            boolean isToday = w.date(i).equals(today);
            List<Week.Slot> slots = w.days.get(i);
            LinearLayout card = Ui.card(this, isToday);
            card.addView(Ui.dayHeader(this, w.date(i), isToday, slots.isEmpty() ? "Nghỉ" : slots.size() + " buổi"));
            if (slots.isEmpty()) {
                card.addView(Ui.text(this, "Không có lịch học", 13, Ui.MUTED, false), Ui.lp(this, 6));
            }
            for (Week.Slot s : slots)
                card.addView(Ui.slot(this, s.buoi, s.gio, s.mon, s.phong, s.gv, s.tiet), Ui.lp(this, 10));
            root.addView(card, Ui.lp(this, 12));
        }
    }

    // ===================== TRỰC LỚP =====================
    private void renderTruc() {
        Duty.Result R = Store.compute(this);
        LocalDate today = LocalDate.now();
        if (R.days.isEmpty()) {
            LinearLayout e = Ui.card(this, false);
            e.addView(Ui.text(this, "Chưa có buổi học", 16, Ui.FG, true));
            e.addView(Ui.text(this, "Lịch trực bắt đầu từ " + Ui.day(Store.start(this)) + ". Bấm Cập nhật ngay ở tab Lịch học hoặc thêm buổi học bù ở tab Lớp.", 14, Ui.MUTED, false), Ui.lp(this, 4));
            root.addView(e);
            return;
        }

        Duty.Row due = null; Duty.Day dueDay = null; int dueCount = 0;
        for (Duty.Day d : R.days)
            for (Duty.Row r : d.rows)
                if (!r.s.checked && !d.date.isAfter(today)) { dueCount++; if (due == null) { due = r; dueDay = d; } }
        if (due != null) {
            Ui.Buoi st = Ui.buoi(due.s.buoi);
            LinearLayout ban = Ui.vbox(this);
            ban.setBackground(Ui.round(this, Ui.WARN_SOFT, 18));
            ban.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 16));
            ban.addView(Ui.label(this, dueCount > 1 ? "Cần điểm danh · còn " + dueCount + " buổi" : "Cần điểm danh", Ui.WARN));
            ban.addView(Ui.text(this, st.icon + "  " + due.s.mon, 18, Ui.FG, true), Ui.lp(this, 6));
            ban.addView(Ui.text(this, Ui.day(dueDay.date) + " · " + due.s.buoi + " " + due.s.gio, 14, Ui.MUTED, false), Ui.lp(this, 2));
            Button go = Ui.button(this, "Điểm danh ngay", true);
            final String id = due.s.id;
            go.setOnClickListener(v -> openAttendance(id));
            ban.addView(go, Ui.lp(this, 12));
            root.addView(ban);
        }

        Button share = Ui.button(this, "Gửi lịch trực cho lớp", false);
        share.setOnClickListener(v -> shareDuty(R));
        root.addView(share, Ui.lp(this, 12));

        for (Duty.Day d : R.days) {
            if (d.date.isBefore(today.minusDays(7))) continue;
            boolean isToday = d.date.equals(today);
            LinearLayout card = Ui.card(this, isToday);
            card.addView(Ui.dayHeader(this, d.date, isToday, null));

            LinearLayout crewBox = Ui.vbox(this);
            crewBox.setBackground(Ui.round(this, Ui.BRAND_SOFT, 12));
            crewBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            crewBox.addView(Ui.label(this, "Đến lượt trực", Ui.BRAND));
            crewBox.addView(Ui.text(this, names(R, d.crew0), 14, Ui.FG, true), Ui.lp(this, 3));
            card.addView(crewBox, Ui.lp(this, 10));

            for (Duty.Row r : d.rows) {
                Ui.Buoi st = Ui.buoi(r.s.buoi);
                LinearLayout blk = Ui.vbox(this);
                blk.setBackground(Ui.round(this, st.bg, 14));
                blk.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));

                LinearLayout h = Ui.hbox(this);
                h.addView(Ui.text(this, st.icon + "  " + r.s.gio + "  " + r.s.mon, 15, st.fg, true), Ui.weight(1));
                if (r.s.checked) h.addView(Ui.pill(this, "✓ Đã điểm danh", Ui.OK, Ui.OK_SOFT));
                else if (!d.date.isAfter(today)) h.addView(Ui.pill(this, "Chưa điểm danh", Ui.WARN, Ui.WARN_SOFT));
                else h.addView(Ui.pill(this, "Dự kiến", st.sub, st == Ui.TOI ? 0x33FFFFFF : Color.WHITE));
                blk.addView(h);

                LinearLayout chips = Ui.vbox(this);
                if (r.assigned.isEmpty())
                    chips.addView(Ui.text(this, "Không đủ người có mặt để trực", 13, st.sub, false));
                for (String id : r.assigned) {
                    String line = "• " + R.name(id);
                    if (r.subFor.containsKey(id)) line += "  (thay " + Store.shortName(R, r.subFor.get(id)) + ")";
                    chips.addView(Ui.text(this, line, 14, r.subFor.containsKey(id) ? st.accent : st.fg, r.subFor.containsKey(id)), Ui.lp(this, 3));
                }
                blk.addView(chips, Ui.lp(this, 6));
                if (r.s.checked && !r.absentIds.isEmpty())
                    blk.addView(Ui.text(this, "Vắng " + r.absentIds.size() + ": " + names(R, r.absentIds), 12, st == Ui.TOI ? Ui.BAD_SOFT : Ui.BAD, false), Ui.lp(this, 6));

                Button b = Ui.button(this, r.s.checked ? "Sửa điểm danh" : "Điểm danh", !r.s.checked && !d.date.isAfter(today));
                final String id = r.s.id;
                b.setOnClickListener(v -> openAttendance(id));
                blk.addView(b, Ui.wrap(this, 10));
                card.addView(blk, Ui.lp(this, 10));
            }
            root.addView(card, Ui.lp(this, 12));
        }
        TextView note = Ui.text(this, "Lịch phía sau là dự kiến và tự đổi theo điểm danh. Bạn đến lượt mà vắng cả ngày sẽ trực vào ngày học kế tiếp, nên không ai bị bỏ sót.", 12, Ui.MUTED, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note, Ui.lp(this, 14));
    }

    private String names(Duty.Result R, List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (String id : ids) { if (b.length() > 0) b.append(", "); b.append(R.name(id)); }
        return b.toString();
    }

    private void openAttendance(String id) {
        startActivity(new Intent(this, AttendanceActivity.class).putExtra(AttendanceActivity.EXTRA_ID, id));
    }

    private void shareDuty(Duty.Result R) {
        LocalDate today = LocalDate.now();
        StringBuilder b = new StringBuilder("📋 LỊCH TRỰC LỚP TA.CĐ.CNT.1.25\n");
        int n = 0;
        for (Duty.Day d : R.days) {
            if (d.date.isBefore(today) || n >= 7) continue;
            n++;
            Duty.Row last = d.rows.get(0);
            for (Duty.Row r : d.rows) if (r.s.checked) last = r;
            b.append("\n").append(Ui.day(d.date)).append(": ");
            StringBuilder p = new StringBuilder();
            for (String id : last.assigned) { if (p.length() > 0) p.append(", "); p.append(R.name(id)); }
            b.append(p);
        }
        b.append("\n\nLịch có thể đổi nếu bạn đến lượt vắng mặt.");
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, b.toString());
        startActivity(Intent.createChooser(send, "Gửi lịch trực"));
    }

    // ===================== LỚP =====================
    private void renderLop() {
        Duty.Result R = Store.compute(this);
        List<Duty.Student> roster = Store.roster(this);

        LinearLayout rules = Ui.card(this, false);
        rules.addView(Ui.label(this, "Quy định trực", Ui.MUTED));
        LinearLayout stats = Ui.hbox(this);
        stats.addView(stat(String.valueOf(Store.perDay(this)), "bạn / ngày"), Ui.weight(1));
        stats.addView(stat(String.format("%02d/%02d", Store.start(this).getDayOfMonth(), Store.start(this).getMonthValue()), "bắt đầu xếp"), Ui.weight(1));
        stats.addView(stat(String.valueOf(countActive(roster)), "bạn đang trực"), Ui.weight(1));
        rules.addView(stats, Ui.lp(this, 8));
        LinearLayout rb = Ui.hbox(this);
        Button perDay = Ui.button(this, "Đổi số người", false);
        perDay.setOnClickListener(v -> askNumber());
        Button start = Ui.button(this, "Đổi ngày bắt đầu", false);
        start.setOnClickListener(v -> {
            LocalDate d = Store.start(this);
            new DatePickerDialog(this, (dp, y, m, dd) -> { Store.setStart(this, LocalDate.of(y, m + 1, dd)); render(); },
                    d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        rb.addView(perDay, Ui.weight(1));
        LinearLayout.LayoutParams g = Ui.weight(1);
        g.leftMargin = Ui.dp(this, 8);
        rb.addView(start, g);
        rules.addView(rb, Ui.lp(this, 12));
        Button addS = Ui.button(this, "+ Thêm buổi học bù", false);
        addS.setOnClickListener(v -> askSession());
        rules.addView(addS, Ui.lp(this, 8));
        root.addView(rules);

        StringBuilder next = new StringBuilder();
        for (int i = 0; i < Math.min(Store.perDay(this), R.queue.size()); i++) {
            if (i > 0) next.append(", ");
            next.append(R.name(R.queue.get(i)));
        }
        LinearLayout nx = Ui.vbox(this);
        nx.setBackground(Ui.round(this, Ui.BRAND_SOFT, 14));
        nx.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        nx.addView(Ui.label(this, "Lượt kế tiếp sau các buổi đã có", Ui.BRAND));
        nx.addView(Ui.text(this, next.toString(), 14, Ui.FG, true), Ui.lp(this, 4));
        root.addView(nx, Ui.lp(this, 12));

        LinearLayout head = Ui.hbox(this);
        head.addView(Ui.text(this, "Danh sách lớp", 18, Ui.FG, true), Ui.weight(1));
        Button add = Ui.button(this, "+ Thêm bạn", false);
        add.setOnClickListener(v -> askStudent(null));
        head.addView(add);
        root.addView(head, Ui.lp(this, 22));
        root.addView(Ui.text(this, "Ngày trực · buổi trực · trực thay · buổi vắng. Chạm vào tên để sửa hoặc cho nghỉ trực.", 12, Ui.MUTED, false), Ui.lp(this, 4));

        LinearLayout list = Ui.card(this, false);
        list.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        roster.sort((a, b) -> a.stt - b.stt);
        boolean first = true;
        for (Duty.Student s : roster) {
            Duty.Stat st = R.stats.get(s.id);
            if (!first) {
                TextView div = new TextView(this);
                div.setBackgroundColor(Ui.LINE);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, 1);
                dl.leftMargin = Ui.dp(this, 60);
                list.addView(div, dl);
            }
            first = false;
            LinearLayout row = Ui.hbox(this);
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
            row.addView(avatar(s), new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
            LinearLayout nm = Ui.vbox(this);
            nm.addView(Ui.text(this, s.name(), 15, s.active ? Ui.FG : Ui.MUTED, true));
            nm.addView(Ui.text(this, "STT " + s.stt + (s.active ? "" : " · đã nghỉ trực"), 12, Ui.MUTED, false));
            LinearLayout.LayoutParams nl = Ui.weight(1);
            nl.leftMargin = Ui.dp(this, 12);
            row.addView(nm, nl);
            if (st != null) {
                TextView n = Ui.text(this, st.days + " · " + st.sess + " · " + st.subs + " · " + st.absent, 13, Ui.MUTED, false);
                n.setFontFeatureSettings("tnum");
                row.addView(n);
            }
            row.setOnClickListener(v -> askStudent(s));
            list.addView(row);
        }
        root.addView(list, Ui.lp(this, 10));
    }

    private LinearLayout stat(String big, String small) {
        LinearLayout b = Ui.vbox(this);
        TextView t = Ui.text(this, big, 22, Ui.BRAND, true);
        t.setFontFeatureSettings("tnum");
        b.addView(t);
        b.addView(Ui.text(this, small, 12, Ui.MUTED, false));
        return b;
    }

    private TextView avatar(Duty.Student s) {
        String t = s.ten.trim();
        TextView a = Ui.text(this, t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase(), 14, s.active ? Ui.BRAND : Ui.MUTED, true);
        a.setGravity(Gravity.CENTER);
        a.setBackground(Ui.round(this, s.active ? Ui.BRAND_SOFT : Ui.LINE, 99));
        return a;
    }

    private int countActive(List<Duty.Student> l) { int n = 0; for (Duty.Student s : l) if (s.active) n++; return n; }

    private void askNumber() {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(Store.perDay(this)));
        new AlertDialog.Builder(this).setTitle("Số bạn trực mỗi ngày").setView(e)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        int n = Integer.parseInt(e.getText().toString().trim());
                        if (n < 1 || n > 10) throw new NumberFormatException();
                        Store.setPerDay(this, n);
                        render();
                    } catch (NumberFormatException ex) { Toast.makeText(this, "Nhập số từ 1 đến 10", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void askStudent(Duty.Student edit) {
        LinearLayout f = Ui.vbox(this);
        int p = Ui.dp(this, 20);
        f.setPadding(p, Ui.dp(this, 8), p, 0);
        EditText ho = new EditText(this), ten = new EditText(this);
        ho.setHint("Họ và tên lót");
        ten.setHint("Tên");
        if (edit != null) { ho.setText(edit.ho); ten.setText(edit.ten); }
        f.addView(ho);
        f.addView(ten);
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(edit == null ? "Thêm bạn vào lớp" : "Sửa: " + edit.name()).setView(f)
                .setPositiveButton("Lưu", (d, w) -> {
                    String h = ho.getText().toString().trim(), t = ten.getText().toString().trim();
                    if (t.isEmpty()) { Toast.makeText(this, "Nhập tên", Toast.LENGTH_SHORT).show(); return; }
                    List<Duty.Student> list = Store.roster(this);
                    if (edit == null) {
                        Duty.Student s = new Duty.Student();
                        int max = 0;
                        for (Duty.Student x : list) max = Math.max(max, x.stt);
                        s.stt = max + 1;
                        s.id = "SV" + System.currentTimeMillis();
                        s.ho = h; s.ten = t;
                        list.add(s);
                    } else for (Duty.Student x : list) if (x.id.equals(edit.id)) { x.ho = h; x.ten = t; }
                    Store.saveRoster(this, list);
                    render();
                })
                .setNegativeButton("Hủy", null);
        if (edit != null)
            b.setNeutralButton(edit.active ? "Cho nghỉ trực" : "Cho trực lại", (d, w) -> {
                List<Duty.Student> list = Store.roster(this);
                for (Duty.Student x : list) if (x.id.equals(edit.id)) x.active = !x.active;
                Store.saveRoster(this, list);
                render();
            });
        b.show();
    }

    private void askSession() {
        LocalDate today = LocalDate.now();
        new DatePickerDialog(this, (dp, y, m, dd) -> {
            LocalDate date = LocalDate.of(y, m + 1, dd);
            LinearLayout f = Ui.vbox(this);
            int p = Ui.dp(this, 20);
            f.setPadding(p, Ui.dp(this, 8), p, 0);
            Spinner buoi = new Spinner(this);
            buoi.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Duty.BUOI));
            EditText mon = new EditText(this), phong = new EditText(this);
            mon.setHint("Tên môn");
            phong.setHint("Phòng (không bắt buộc)");
            f.addView(buoi);
            f.addView(mon);
            f.addView(phong);
            new AlertDialog.Builder(this).setTitle("Buổi học bù · " + Ui.day(date)).setView(f)
                    .setPositiveButton("Thêm", (d, w) -> {
                        String bn = (String) buoi.getSelectedItem();
                        Duty.Session s = new Duty.Session();
                        s.date = date;
                        s.buoi = bn;
                        s.id = Duty.sessionId(date, bn) + "_them";
                        s.gio = bn.equals("Sáng") ? "7:00" : bn.equals("Chiều") ? "13:00" : "17:30";
                        s.mon = mon.getText().toString().trim().isEmpty() ? "Học bù" : mon.getText().toString().trim();
                        s.phong = phong.getText().toString().trim();
                        Store.addSession(this, s);
                        Toast.makeText(this, "Đã thêm buổi " + bn + " " + Ui.day(date), Toast.LENGTH_SHORT).show();
                        render();
                    })
                    .setNegativeButton("Hủy", null).show();
        }, today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth()).show();
    }
}
