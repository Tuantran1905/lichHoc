package vn.lichhoc.cnt125;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Điểm danh một buổi học: chạm vào tên để đổi Có mặt / Vắng, rồi Lưu. */
public class AttendanceActivity extends Activity {
    public static final String EXTRA_ID = "session";
    private final Set<String> absent = new LinkedHashSet<>();
    private final Map<String, LinearLayout> rows = new HashMap<>();
    private final Map<String, Duty.Student> studs = new HashMap<>();
    private TextView present, away;
    private int total;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra(EXTRA_ID);
        Duty.Session s = id == null ? null : Store.session(this, id);
        if (s == null) { finish(); return; }
        absent.addAll(s.absent);
        Ui.Buoi st = Ui.buoi(s.buoi);
        getWindow().setStatusBarColor(st == Ui.TOI ? Ui.TOI.bg : Ui.BRAND);

        Duty.Result R = Store.compute(this);
        Duty.Day day = R.day(s.date);
        List<String> crew = day == null ? new ArrayList<>() : day.crew0;

        LinearLayout outer = Ui.vbox(this);
        outer.setBackgroundColor(Ui.BG);
        outer.setFitsSystemWindows(true);

        // Đầu trang tô theo buổi học
        LinearLayout head = Ui.vbox(this);
        head.setBackgroundColor(st == Ui.TOI ? Ui.TOI.bg : Ui.BRAND);
        int p = Ui.dp(this, 20);
        head.setPadding(p, Ui.dp(this, 18), p, Ui.dp(this, 18));
        TextView lb = Ui.text(this, ("Điểm danh · " + st.icon + " " + st.label), 11, Color.argb(200, 255, 255, 255), true);
        lb.setLetterSpacing(0.1f);
        head.addView(lb);
        head.addView(Ui.text(this, s.mon, 22, Color.WHITE, true), Ui.lp(this, 4));
        head.addView(Ui.text(this, Ui.day(s.date) + " · " + s.gio + (s.phong.isEmpty() ? "" : " · " + s.phong), 14, Color.argb(220, 255, 255, 255), false), Ui.lp(this, 2));

        LinearLayout counts = Ui.hbox(this);
        present = Ui.pill(this, "", Ui.OK, Ui.OK_SOFT);
        away = Ui.pill(this, "", Ui.BAD, Ui.BAD_SOFT);
        counts.addView(present);
        counts.addView(away, Ui.gapLeft(this, 8));
        TextView spacer = new TextView(this);
        counts.addView(spacer, Ui.weight(1));
        TextView allIn = Ui.pill(this, "Tất cả có mặt", Color.WHITE, Color.argb(60, 255, 255, 255));
        allIn.setOnClickListener(v -> { absent.clear(); for (String k : rows.keySet()) paint(k); updateCounts(); });
        counts.addView(allIn);
        head.addView(counts, Ui.lp(this, 14));
        outer.addView(head);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = Ui.vbox(this);
        int g = Ui.dp(this, 16);
        list.setPadding(g, g, g, g);
        sv.addView(list);
        outer.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        List<Duty.Student> active = new ArrayList<>();
        for (Duty.Student x : Store.roster(this)) if (x.active) active.add(x);
        active.sort((a, c) -> {
            int ca = crew.contains(a.id) ? 0 : 1, cc = crew.contains(c.id) ? 0 : 1;
            return ca != cc ? ca - cc : a.stt - c.stt;
        });
        total = active.size();

        LinearLayout group = null;
        Boolean groupCrew = null;
        for (Duty.Student x : active) {
            boolean isCrew = crew.contains(x.id);
            if (groupCrew == null || groupCrew != isCrew) {
                list.addView(Ui.label(this, isCrew ? "Đến lượt trực hôm nay" : "Cả lớp", isCrew ? Ui.BRAND : Ui.MUTED), Ui.lp(this, groupCrew == null ? 0 : 18));
                group = Ui.card(this, isCrew);
                group.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
                list.addView(group, Ui.lp(this, 8));
                groupCrew = isCrew;
            } else {
                TextView div = new TextView(this);
                div.setBackgroundColor(Ui.LINE);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, 1);
                dl.leftMargin = Ui.dp(this, 62);
                group.addView(div, dl);
            }
            studs.put(x.id, x);
            LinearLayout row = Ui.hbox(this);
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
            TextView av = Ui.text(this, "", 14, Ui.BRAND, true);
            av.setGravity(Gravity.CENTER);
            av.setTag("av");
            row.addView(av, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
            LinearLayout nm = Ui.vbox(this);
            nm.addView(Ui.text(this, x.name(), 15, Ui.FG, true));
            nm.addView(Ui.text(this, "STT " + x.stt, 12, Ui.MUTED, false));
            LinearLayout.LayoutParams nl = Ui.weight(1);
            nl.leftMargin = Ui.dp(this, 12);
            row.addView(nm, nl);
            TextView tag = Ui.pill(this, "", Ui.OK, Ui.OK_SOFT);
            tag.setTag("tag");
            row.addView(tag);
            row.setOnClickListener(v -> {
                if (!absent.remove(x.id)) absent.add(x.id);
                paint(x.id);
                updateCounts();
            });
            rows.put(x.id, row);
            group.addView(row);
            paint(x.id);
        }

        LinearLayout foot = Ui.hbox(this);
        foot.setBackgroundColor(Ui.CARD);
        foot.setElevation(Ui.dp(this, 8));
        foot.setPadding(g, Ui.dp(this, 12), g, Ui.dp(this, 12));
        Button cancel = Ui.button(this, "Hủy", false);
        cancel.setOnClickListener(v -> finish());
        Button save = Ui.button(this, "Lưu điểm danh", true);
        save.setOnClickListener(v -> {
            Store.saveAttendance(this, s.id, absent);
            TkbWidget.refreshAll(this);
            Toast.makeText(this, "Đã lưu điểm danh · vắng " + absent.size(), Toast.LENGTH_SHORT).show();
            finish();
        });
        foot.addView(cancel, Ui.weight(1));
        LinearLayout.LayoutParams sl = Ui.weight(2);
        sl.leftMargin = Ui.dp(this, 10);
        foot.addView(save, sl);
        outer.addView(foot);

        setContentView(outer);
        updateCounts();
    }

    private void paint(String id) {
        LinearLayout row = rows.get(id);
        Duty.Student x = studs.get(id);
        if (row == null || x == null) return;
        boolean off = absent.contains(id);
        row.setBackgroundColor(off ? Ui.BAD_SOFT : Color.TRANSPARENT);
        TextView tag = row.findViewWithTag("tag");
        tag.setText(off ? "Vắng" : "Có mặt");
        tag.setTextColor(off ? Color.WHITE : Ui.OK);
        tag.setBackground(Ui.round(this, off ? Ui.BAD : Ui.OK_SOFT, 99));
        TextView av = row.findViewWithTag("av");
        String t = x.ten.trim();
        av.setText(t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase());
        av.setTextColor(off ? Ui.BAD : Ui.BRAND);
        av.setBackground(Ui.round(this, off ? Color.WHITE : Ui.BRAND_SOFT, 99));
    }

    private void updateCounts() {
        int a = 0;
        for (String id : absent) if (rows.containsKey(id)) a++;
        present.setText("Có mặt " + (total - a));
        away.setText("Vắng " + a);
    }
}
