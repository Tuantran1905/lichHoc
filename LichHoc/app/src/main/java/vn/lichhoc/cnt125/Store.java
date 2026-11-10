package vn.lichhoc.cnt125;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lưu danh sách lớp, các buổi học, điểm danh và quy định trực trên điện thoại. */
public class Store {
    private static final Object LOCK = new Object();
    static final LocalDate DEFAULT_START = LocalDate.of(2026, 11, 11);

    static SharedPreferences p(Context c) { return c.getSharedPreferences("truc", Context.MODE_PRIVATE); }

    public static List<Duty.Student> roster(Context c) {
        String t = p(c).getString("roster", null);
        if (t == null) {
            List<Duty.Student> d = Roster.defaults();
            saveRoster(c, d);
            return d;
        }
        return Duty.loadRoster(t);
    }

    public static void saveRoster(Context c, List<Duty.Student> list) {
        p(c).edit().putString("roster", Duty.saveRoster(list)).apply();
    }

    public static List<Duty.Session> sessions(Context c) {
        synchronized (LOCK) { return Duty.loadSessions(p(c).getString("sessions", "")); }
    }

    public static void saveSessions(Context c, List<Duty.Session> list) {
        synchronized (LOCK) { p(c).edit().putString("sessions", Duty.saveSessions(list)).commit(); }
    }

    public static Duty.Session session(Context c, String id) {
        for (Duty.Session s : sessions(c)) if (s.id.equals(id)) return s;
        return null;
    }

    public static void saveAttendance(Context c, String id, java.util.Set<String> absent) {
        synchronized (LOCK) {
            List<Duty.Session> all = sessions(c);
            for (Duty.Session s : all)
                if (s.id.equals(id)) { s.checked = true; s.absent.clear(); s.absent.addAll(absent); }
            saveSessions(c, all);
        }
    }

    public static void addSession(Context c, Duty.Session s) {
        synchronized (LOCK) {
            List<Duty.Session> all = sessions(c);
            all.removeIf(x -> x.id.equals(s.id));
            all.add(s);
            saveSessions(c, all);
        }
    }

    public static void deleteSession(Context c, String id) {
        synchronized (LOCK) {
            List<Duty.Session> all = sessions(c);
            all.removeIf(x -> x.id.equals(id));
            saveSessions(c, all);
        }
    }

    /** Gọi sau mỗi lần tải TKB: thêm/cập nhật buổi học của tuần này và tuần sau. */
    public static void mergeWeeks(Context c, Week cur, Week nxt) {
        synchronized (LOCK) {
            List<Duty.Session> all = sessions(c);
            LocalDate today = LocalDate.now();
            all = Duty.merge(all, cur, today);
            all = Duty.merge(all, nxt, today);
            saveSessions(c, all);
        }
    }

    public static int perDay(Context c) { return p(c).getInt("perDay", 3); }
    public static void setPerDay(Context c, int n) { p(c).edit().putInt("perDay", n).apply(); }

    public static LocalDate start(Context c) {
        String s = p(c).getString("start", null);
        return s == null ? DEFAULT_START : LocalDate.parse(s);
    }
    public static void setStart(Context c, LocalDate d) { p(c).edit().putString("start", d.toString()).apply(); }

    public static Duty.Result compute(Context c) {
        return Duty.compute(roster(c), sessions(c), perDay(c), start(c));
    }

    /** Ai trực hôm nay (theo buổi gần nhất đã/đang diễn ra), dùng cho widget. */
    public static List<String> todayCrew(Context c) {
        Duty.Result r = compute(c);
        Duty.Day d = r.day(LocalDate.now());
        List<String> out = new ArrayList<>();
        if (d == null) return out;
        Duty.Row pick = d.rows.get(0);
        for (Duty.Row row : d.rows) if (row.s.checked) pick = row;
        for (String id : pick.assigned) out.add(shortName(r, id));
        return out;
    }

    static String shortName(Duty.Result r, String id) {
        Duty.Student s = r.byId.get(id);
        if (s == null) return id;
        String[] h = s.ho.trim().split("\\s+");
        return (h.length > 0 && !h[h.length - 1].isEmpty() ? h[h.length - 1] + " " : "") + s.ten.trim();
    }
}
