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

    public static Duty.Baseline baseline(Context c) {
        return Duty.loadBaseline(p(c).getString("baseline", null));
    }

    /** Thứ 2 của tuần hiện tại: điểm danh trước ngày này là "tuần đã qua". */
    public static LocalDate weekStart() {
        LocalDate t = LocalDate.now();
        return t.minusDays(t.getDayOfWeek().getValue() - 1);
    }

    /**
     * Chốt các tuần đã qua: tính hàng chờ + thống kê tới hết tuần trước, lưu lại làm mốc,
     * rồi xoá điểm danh các tuần đó khỏi máy. Lượt trực vẫn liên tục như trước khi xoá.
     */
    public static void compactPastWeeks(Context c) {
        synchronized (LOCK) {
            LocalDate cutoff = weekStart();
            List<Duty.Session> all = sessions(c);
            List<Duty.Session> old = new ArrayList<>(), keep = new ArrayList<>();
            for (Duty.Session s : all) (s.date != null && s.date.isBefore(cutoff) ? old : keep).add(s);
            if (old.isEmpty()) return;

            Duty.Baseline base = baseline(c);
            Duty.Result r = Duty.compute(roster(c), old, perDay(c), start(c), base); // compute bỏ qua buổi < base.until
            Duty.Baseline nb = new Duty.Baseline();
            nb.until = base != null && base.until != null && base.until.isAfter(cutoff) ? base.until : cutoff;
            nb.queue = new ArrayList<>(r.queue);
            nb.stats.putAll(r.stats);
            p(c).edit()
                    .putString("baseline", Duty.saveBaseline(nb))
                    .putString("sessions", Duty.saveSessions(keep))
                    .commit();
        }
    }

    public static Duty.Result compute(Context c) {
        compactPastWeeks(c);
        return Duty.compute(roster(c), sessions(c), perDay(c), start(c), baseline(c));
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
