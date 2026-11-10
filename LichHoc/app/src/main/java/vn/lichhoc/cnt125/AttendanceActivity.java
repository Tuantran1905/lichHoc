package vn.lichhoc.cnt125;

import android.app.Activity;
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
    private TextView counts;
    private int total;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra(EXTRA_ID);
        Duty.Session s = id == null ? null : Store.session(this, id);
        if (s == null) { finish(); return; }
        absent.addAll(s.absent);

        Duty.Result R = Store.compute(this);
        Duty.Day day = R.day(s.date);
        List<String> crew = day == null ? new ArrayList<>() : day.crew0;

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Ui.BG);
        outer.setFitsSystemWindows(true);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        head.setPadding(p, p, p, Ui.dp(this, 8));
        head.addView(Ui.text(this, "ĐIỂM DANH", 11, Ui.MUTED, true));
        head.addView(Ui.text(this, Ui.day(s.date) + " · " + s.buoi + " " + s.gio, 18, Ui.FG, true));
        head.addView(Ui.text(this, s.mon + (s.phong.isEmpty() ? "" : " · " + s.phong), 14, Ui.MUTED, false));
        counts = Ui.text(this, "", 14, Ui.FG, false);
        head.addView(counts, Ui.lp(this, 6));
        Button allIn = Ui.button(this, "Tất cả có mặt", false);
        allIn.setOnClickListener(v -> { absent.clear(); for (String k : rows.keySet()) paint(k); updateCounts(); });
        head.addView(allIn, Ui.wrap(this, 8));
        outer.addView(head);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(p, 0, p, p);
        sv.addView(list);
        outer.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        List<Duty.Student> studs = new ArrayList<>();
        for (Duty.Student st : Store.roster(this)) if (st.active) studs.add(st);
        studs.sort((a, c) -> {
            int ca = crew.contains(a.id) ? 0 : 1, cc = crew.contains(c.id) ? 0 : 1;
            return ca != cc ? ca - cc : a.stt - c.stt;
        });
        total = studs.size();
        if (!crew.isEmpty()) list.addView(Ui.text(this, "Đến lượt trực hôm nay", 12, Ui.ACCENT, true), Ui.lp(this, 4));
        boolean shownRest = false;
        for (Duty.Student st : studs) {
            if (!crew.contains(st.id) && !shownRest && !crew.isEmpty()) {
                list.addView(Ui.text(this, "Cả lớp", 12, Ui.MUTED, true), Ui.lp(this, 12));
                shownRest = true;
            }
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            TextView stt = Ui.text(this, String.valueOf(st.stt), 13, Ui.MUTED, false);
            row.addView(stt, new LinearLayout.LayoutParams(Ui.dp(this, 30), -2));
            row.addView(Ui.text(this, st.name(), 15, Ui.FG, false), new LinearLayout.LayoutParams(0, -2, 1));
            TextView tag = Ui.text(this, "", 12, Ui.OK, true);
            tag.setTag("tag");
            row.addView(tag);
            row.setOnClickListener(v -> {
                if (!absent.remove(st.id)) absent.add(st.id);
                paint(st.id);
                updateCounts();
            });
            rows.put(st.id, row);
            list.addView(row, Ui.lp(this, 6));
            paint(st.id);
        }

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.END);
        foot.setPadding(p, Ui.dp(this, 10), p, p);
        Button cancel = Ui.button(this, "Hủy", false);
        cancel.setOnClickListener(v -> finish());
        Button save = Ui.button(this, "Lưu điểm danh", true);
        save.setOnClickListener(v -> {
            Store.saveAttendance(this, s.id, absent);
            TkbWidget.refreshAll(this);
            Toast.makeText(this, "Đã lưu điểm danh", Toast.LENGTH_SHORT).show();
            finish();
        });
        foot.addView(cancel);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
        sl.leftMargin = Ui.dp(this, 8);
        foot.addView(save, sl);
        outer.addView(foot);

        setContentView(outer);
        updateCounts();
    }

    private void paint(String id) {
        LinearLayout row = rows.get(id);
        if (row == null) return;
        boolean off = absent.contains(id);
        row.setBackground(off ? Ui.round(this, Ui.BAD_SOFT, 10) : Ui.round(this, Ui.CARD, 10));
        TextView tag = row.findViewWithTag("tag");
        tag.setText(off ? "Vắng" : "Có mặt");
        tag.setTextColor(off ? Ui.BAD : Ui.OK);
    }

    private void updateCounts() {
        int a = 0;
        for (String id : absent) if (rows.containsKey(id)) a++;
        counts.setText("Có mặt " + (total - a) + " · Vắng " + a);
    }
}
