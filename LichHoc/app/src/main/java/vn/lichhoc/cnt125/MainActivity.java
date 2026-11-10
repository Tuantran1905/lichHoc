package vn.lichhoc.cnt125;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.time.LocalDate;
import java.util.List;

/** Ba tab: Lịch học, Trực lớp (điểm danh + người trực), Lớp (danh sách, thống kê, quy định). */
public class MainActivity extends Activity {
    private LinearLayout root;
    private ScrollView sv;
    private String tab = "lich";
    private Button refresh;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Repo.ensureChannel(this);
        SyncJob.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        if (b != null) tab = b.getString("tab", "lich");

        sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFitsSystemWindows(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p * 2);
        sv.addView(root);
        setContentView(sv);
        render();
        if (Repo.current(this) == null && Repo.next(this) == null) syncNow();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("tab", tab); }

    @Override
    protected void onResume() { super.onResume(); render(); }

    private void syncNow() {
        if (refresh != null) { refresh.setEnabled(false); refresh.setText("Đang tải lịch…"); }
        new Thread(() -> {
            String err = Repo.sync(getApplicationContext());
            runOnUiThread(() -> {
                render();
                Toast.makeText(this, err == null ? "Đã cập nhật lịch" : "Lỗi: " + err, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void render() {
        root.removeAllViews();
        root.addView(Ui.text(this, "LỚP TRƯỞNG · TA.CĐ.CNT.1.25", 11, Ui.MUTED, true));
        root.addView(tabs(), Ui.lp(this, 10));
        if (tab.equals("truc")) renderTruc();
        else if (tab.equals("lop")) renderLop();
        else renderLich();
    }

    private LinearLayout tabs() {
        LinearLayout t = new LinearLayout(this);
        t.setBackground(Ui.round(this, Ui.CARD, 12));
        int p = Ui.dp(this, 4);
        t.setPadding(p, p, p, p);
        String[][] items = {{"lich", "Lịch học"}, {"truc", "Trực lớp"}, {"lop", "Lớp"}};
        for (String[] it : items) {
            TextView b = Ui.text(this, it[1], 14, it[0].equals(tab) ? Ui.FG : Ui.MUTED, true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
            if (it[0].equals(tab)) b.setBackground(Ui.round(this, Ui.ACCENT_SOFT, 9));
            b.setOnClickListener(v -> { tab = it[0]; render(); sv.scrollTo(0, 0); });
            t.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return t;
    }

    // ===================== LỊCH HỌC =====================
    private void renderLich() {
        Week cur = Repo.current(this), nxt = Repo.next(this);
        Week any = cur != null ? cur : nxt;
        if (any != null && !any.cvht.isEmpty()) root.addView(Ui.text(this, "CVHT " + any.cvht, 13, Ui.MUTED, false), Ui.lp(this, 12));

        String change = Repo.lastChange(this);
        if (!change.isEmpty()) {
            TextView t = Ui.text(this, "Thay đổi mới: " + change, 14, Ui.FG, false);
            t.setBackground(Ui.round(this, Ui.WARN_SOFT, 12));
            t.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
            root.addView(t, Ui.lp(this, 12));
        }
        if (cur != null) addWeek("Tuần này", cur);
        if (nxt != null) addWeek("Tuần sau", nxt);
        if (cur == null && nxt == null)
            root.addView(Ui.text(this, "Chưa có lịch. Bấm \"Cập nhật ngay\" để tải từ file TKB của trường.", 14, Ui.MUTED, false), Ui.lp(this, 20));

        refresh = Ui.button(this, "Cập nhật ngay", true);
        refresh.setOnClickListener(v -> syncNow());
        root.addView(refresh, Ui.lp(this, 20));
        String info = "Tự kiểm tra khoảng 2 giờ một lần khi có mạng.";
        if (!Repo.lastSync(this).isEmpty()) info = "Kiểm tra lần cuối: " + Repo.lastSync(this) + "\n" + info;
        if (!Repo.lastError(this).isEmpty()) info = "Lỗi lần trước: " + Repo.lastError(this) + "\n" + info;
        root.addView(Ui.text(this, info, 12, Ui.MUTED, false), Ui.lp(this, 8));
    }

    private void addWeek(String title, Week w) {
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(this, title + " · " + w.tuan(), 17, Ui.FG, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (!w.statusLabel().isEmpty())
            head.addView(w.isFinal() ? Ui.pill(this, w.statusLabel(), Ui.OK, Ui.OK_SOFT) : Ui.pill(this, w.statusLabel(), Ui.WARN, Ui.WARN_SOFT));
        root.addView(head, Ui.lp(this, 20));
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            boolean isToday = w.date(i).equals(today);
            LinearLayout card = Ui.card(this, isToday);
            card.addView(Ui.text(this, Ui.day(w.date(i)) + (isToday ? "  · Hôm nay" : ""), 15, isToday ? Ui.ACCENT : Ui.FG, true));
            List<Week.Slot> slots = w.days.get(i);
            if (slots.isEmpty()) card.addView(Ui.text(this, "Không có lịch", 13, Ui.MUTED, false));
            for (Week.Slot s : slots) {
                card.addView(Ui.text(this, s.buoi + " " + s.gio + "  " + s.mon, 15, Ui.FG, true), Ui.lp(this, 6));
                String meta = s.gv + (s.phong.isEmpty() ? "" : "  ·  " + s.phong) + (s.tiet.isEmpty() ? "" : "  ·  " + s.tiet);
                card.addView(Ui.text(this, meta, 13, Ui.MUTED, false));
            }
            root.addView(card, Ui.lp(this, 10));
        }
    }

    // ===================== TRỰC LỚP =====================
    private void renderTruc() {
        Duty.Result R = Store.compute(this);
        LocalDate today = LocalDate.now();
        if (R.days.isEmpty()) {
            root.addView(Ui.text(this, "Chưa có buổi học nào từ " + Ui.day(Store.start(this)) + ". Bấm Cập nhật ngay ở tab Lịch học hoặc thêm buổi ở tab Lớp.", 14, Ui.MUTED, false), Ui.lp(this, 20));
            return;
        }

        Duty.Row due = null; Duty.Day dueDay = null; int dueCount = 0;
        for (Duty.Day d : R.days)
            for (Duty.Row r : d.rows)
                if (!r.s.checked && !d.date.isAfter(today)) { dueCount++; if (due == null) { due = r; dueDay = d; } }
        if (due != null) {
            LinearLayout ban = new LinearLayout(this);
            ban.setOrientation(LinearLayout.VERTICAL);
            ban.setBackground(Ui.round(this, Ui.WARN_SOFT, 14));
            ban.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            ban.addView(Ui.text(this, "CẦN ĐIỂM DANH", 11, Ui.WARN, true));
            ban.addView(Ui.text(this, Ui.day(dueDay.date) + " · " + due.s.buoi + " " + due.s.gio + " · " + due.s.mon, 15, Ui.FG, true), Ui.lp(this, 4));
            if (dueCount > 1) ban.addView(Ui.text(this, "Còn " + dueCount + " buổi chưa điểm danh.", 13, Ui.MUTED, false));
            Button go = Ui.button(this, "Điểm danh buổi này", true);
            final String id = due.s.id;
            go.setOnClickListener(v -> openAttendance(id));
            ban.addView(go, Ui.wrap(this, 8));
            root.addView(ban, Ui.lp(this, 14));
        }

        Button share = Ui.button(this, "Gửi lịch trực cho lớp", false);
        share.setOnClickListener(v -> shareDuty(R));
        root.addView(share, Ui.wrap(this, 12));

        for (Duty.Day d : R.days) {
            if (d.date.isBefore(today.minusDays(7))) continue;
            boolean isToday = d.date.equals(today);
            LinearLayout card = Ui.card(this, isToday);
            card.addView(Ui.text(this, Ui.day(d.date) + (isToday ? "  · Hôm nay" : ""), 15, isToday ? Ui.ACCENT : Ui.FG, true));
            card.addView(Ui.text(this, "Đến lượt: " + names(R, d.crew0), 13, Ui.MUTED, false), Ui.lp(this, 2));
            for (Duty.Row r : d.rows) {
                LinearLayout h = new LinearLayout(this);
                h.setGravity(Gravity.CENTER_VERTICAL);
                h.addView(Ui.text(this, r.s.buoi + " " + r.s.gio + " · " + r.s.mon, 14, Ui.FG, true), new LinearLayout.LayoutParams(0, -2, 1));
                if (r.s.checked) h.addView(Ui.pill(this, "Đã điểm danh", Ui.OK, Ui.OK_SOFT));
                else if (!d.date.isAfter(today)) h.addView(Ui.pill(this, "Chưa điểm danh", Ui.WARN, Ui.WARN_SOFT));
                else h.addView(Ui.pill(this, "Dự kiến", Ui.MUTED, Ui.LINE));
                card.addView(h, Ui.lp(this, 10));

                StringBuilder ppl = new StringBuilder();
                for (String id : r.assigned) {
                    if (ppl.length() > 0) ppl.append(", ");
                    ppl.append(R.name(id));
                    if (r.subFor.containsKey(id)) ppl.append(" (thay ").append(Store.shortName(R, r.subFor.get(id))).append(")");
                }
                card.addView(Ui.text(this, "Trực: " + (ppl.length() == 0 ? "không đủ người có mặt" : ppl), 14, Ui.FG, false), Ui.lp(this, 2));
                if (r.s.checked && !r.absentIds.isEmpty())
                    card.addView(Ui.text(this, "Vắng (" + r.absentIds.size() + "): " + names(R, r.absentIds), 12, Ui.BAD, false));
                Button b = Ui.button(this, r.s.checked ? "Sửa điểm danh" : "Điểm danh", !r.s.checked && !d.date.isAfter(today));
                final String id = r.s.id;
                b.setOnClickListener(v -> openAttendance(id));
                card.addView(b, Ui.wrap(this, 6));
            }
            root.addView(card, Ui.lp(this, 10));
        }
        root.addView(Ui.text(this, "Lịch phía sau là dự kiến và tự đổi theo điểm danh. Bạn đến lượt mà vắng cả ngày sẽ trực vào ngày học kế tiếp, nên không ai bị bỏ sót.", 12, Ui.MUTED, false), Ui.lp(this, 12));
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
        rules.addView(Ui.text(this, "Quy định trực", 16, Ui.FG, true));
        rules.addView(Ui.text(this, "Mỗi ngày có học: " + Store.perDay(this) + " bạn · Bắt đầu xếp từ " + Ui.day(Store.start(this)), 14, Ui.FG, false), Ui.lp(this, 4));
        LinearLayout rb = new LinearLayout(this);
        Button perDay = Ui.button(this, "Đổi số người", false);
        perDay.setOnClickListener(v -> askNumber());
        Button start = Ui.button(this, "Đổi ngày bắt đầu", false);
        start.setOnClickListener(v -> {
            LocalDate d = Store.start(this);
            new DatePickerDialog(this, (dp, y, m, dd) -> { Store.setStart(this, LocalDate.of(y, m + 1, dd)); render(); },
                    d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        rb.addView(perDay);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(-2, -2);
        gap.leftMargin = Ui.dp(this, 8);
        rb.addView(start, gap);
        rules.addView(rb, Ui.lp(this, 8));
        Button addS = Ui.button(this, "Thêm buổi học bù", false);
        addS.setOnClickListener(v -> askSession());
        rules.addView(addS, Ui.wrap(this, 8));
        root.addView(rules, Ui.lp(this, 14));

        StringBuilder next = new StringBuilder();
        for (int i = 0; i < Math.min(Store.perDay(this), R.queue.size()); i++) {
            if (i > 0) next.append(", ");
            next.append(R.name(R.queue.get(i)));
        }
        root.addView(Ui.text(this, "Lượt kế tiếp sau các buổi đã có: " + next, 13, Ui.MUTED, false), Ui.lp(this, 10));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(this, "Danh sách lớp (" + countActive(roster) + " bạn)", 16, Ui.FG, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = Ui.button(this, "Thêm bạn", false);
        add.setOnClickListener(v -> askStudent(null));
        head.addView(add);
        root.addView(head, Ui.lp(this, 18));
        root.addView(Ui.text(this, "Số trong ngoặc: ngày trực · buổi trực · trực thay · buổi vắng. Chạm vào tên để sửa.", 12, Ui.MUTED, false), Ui.lp(this, 4));

        roster.sort((a, b) -> a.stt - b.stt);
        for (Duty.Student s : roster) {
            Duty.Stat st = R.stats.get(s.id);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.round(this, s.active ? Ui.CARD : Ui.LINE, 10));
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
            row.addView(Ui.text(this, String.valueOf(s.stt), 13, Ui.MUTED, false), new LinearLayout.LayoutParams(Ui.dp(this, 30), -2));
            row.addView(Ui.text(this, s.name() + (s.active ? "" : " · đã nghỉ"), 14, s.active ? Ui.FG : Ui.MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
            if (st != null) row.addView(Ui.text(this, st.days + " · " + st.sess + " · " + st.subs + " · " + st.absent, 13, Ui.MUTED, false));
            row.setOnClickListener(v -> askStudent(s));
            root.addView(row, Ui.lp(this, 6));
        }
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
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
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
            LinearLayout f = new LinearLayout(this);
            f.setOrientation(LinearLayout.VERTICAL);
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
