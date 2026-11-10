package vn.lichhoc.cnt125;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Điểm danh từng buổi và xếp người trực lớp. Không phụ thuộc Android.
 *
 * Quy tắc: hàng chờ ban đầu theo STT. Mỗi ngày có học, N bạn đầu hàng chờ là "đến lượt".
 * Ở mỗi buổi đã điểm danh, bạn đến lượt có mặt thì trực; bạn vắng được thay bằng người kế tiếp
 * đang có mặt (ưu tiên người đã trực thay trong ngày, rồi theo hàng chờ). Buổi chưa điểm danh
 * là dự kiến: giữ nguyên người trực của buổi đã điểm danh gần nhất trong ngày.
 * Hết ngày, ai đã trực thì xuống cuối hàng chờ; ai đến lượt mà vắng cả ngày giữ chỗ đầu hàng.
 */
public class Duty {
    public static final String[] BUOI = {"Sáng", "Chiều", "Tối"};
    public static final String[] BUOI_KEY = {"sang", "chieu", "toi"};

    public static class Student {
        public String id = "", ho = "", ten = "";
        public int stt;
        public boolean active = true;
        public String name() { return (ho + " " + ten).replaceAll("\\s+", " ").trim(); }
    }

    public static class Session {
        public String id = "", buoi = "", gio = "", mon = "", phong = "", gv = "";
        public LocalDate date;
        public boolean checked;
        public final Set<String> absent = new LinkedHashSet<>();

        int buoiIndex() { for (int i = 0; i < 3; i++) if (BUOI[i].equals(buoi)) return i; return 3; }
    }

    public static class Row {
        public Session s;
        public final List<String> assigned = new ArrayList<>();
        public final Map<String, String> subFor = new LinkedHashMap<>();
        public final List<String> absentIds = new ArrayList<>();
    }

    public static class Day {
        public LocalDate date;
        public List<String> crew0;
        public final List<Row> rows = new ArrayList<>();
        public boolean allChecked;
    }

    public static class Stat { public int days, sess, absent, subs; }

    public static class Result {
        public final List<Day> days = new ArrayList<>();
        public final Map<String, Stat> stats = new HashMap<>();
        public List<String> queue = new ArrayList<>();
        public final Map<String, Student> byId = new HashMap<>();

        public String name(String id) { Student s = byId.get(id); return s == null ? id : s.name(); }
        public Day day(LocalDate d) { for (Day x : days) if (x.date.equals(d)) return x; return null; }
    }

    public static String sessionId(LocalDate d, String buoi) {
        for (int i = 0; i < 3; i++) if (BUOI[i].equals(buoi)) return d + "_" + BUOI_KEY[i];
        return d + "_" + buoi;
    }

    public static Result compute(List<Student> roster, List<Session> sessions, int perDay, LocalDate start) {
        Result R = new Result();
        int n = Math.max(1, perDay);
        List<String> queue = new ArrayList<>();
        List<Student> sorted = new ArrayList<>(roster);
        sorted.sort(Comparator.comparingInt(s -> s.stt));
        for (Student s : sorted) {
            R.byId.put(s.id, s);
            R.stats.put(s.id, new Stat());
            if (s.active) queue.add(s.id);
        }
        List<Session> ss = new ArrayList<>();
        for (Session s : sessions) if (s.date != null && (start == null || !s.date.isBefore(start))) ss.add(s);
        ss.sort(Comparator.comparing((Session s) -> s.date).thenComparingInt(Session::buoiIndex));

        for (int i = 0; i < ss.size(); ) {
            Day d = new Day();
            d.date = ss.get(i).date;
            List<Session> today = new ArrayList<>();
            while (i < ss.size() && ss.get(i).date.equals(d.date)) today.add(ss.get(i++));
            d.crew0 = new ArrayList<>(queue.subList(0, Math.min(n, queue.size())));
            Set<String> served = new HashSet<>();
            List<String> subsToday = new ArrayList<>();
            Row last = null;
            boolean all = true;
            for (Session s : today) {
                Row r = new Row();
                r.s = s;
                if (s.checked) {
                    for (String id : s.absent) if (R.byId.containsKey(id)) { r.absentIds.add(id); R.stats.get(id).absent++; }
                    for (String id : d.crew0) if (!s.absent.contains(id)) r.assigned.add(id);
                    List<String> cands = new ArrayList<>(subsToday);
                    for (String id : queue.subList(Math.min(n, queue.size()), queue.size())) if (!cands.contains(id)) cands.add(id);
                    for (String miss : d.crew0) {
                        if (!s.absent.contains(miss)) continue;
                        for (String c : cands) {
                            if (s.absent.contains(c) || r.assigned.contains(c) || d.crew0.contains(c)) continue;
                            r.assigned.add(c);
                            r.subFor.put(c, miss);
                            if (!subsToday.contains(c)) subsToday.add(c);
                            break;
                        }
                    }
                    for (String id : r.assigned) {
                        R.stats.get(id).sess++;
                        if (r.subFor.containsKey(id)) R.stats.get(id).subs++;
                    }
                    last = r;
                } else {
                    all = false;
                    if (last != null) { r.assigned.addAll(last.assigned); r.subFor.putAll(last.subFor); }
                    else r.assigned.addAll(d.crew0);
                }
                served.addAll(r.assigned);
                d.rows.add(r);
            }
            d.allChecked = all;
            if (all) for (String id : served) R.stats.get(id).days++;
            List<String> stay = new ArrayList<>(), moved = new ArrayList<>();
            for (String id : queue) (served.contains(id) ? moved : stay).add(id);
            stay.addAll(moved);
            queue = stay;
            R.days.add(d);
        }
        R.queue = queue;
        return R;
    }

    // ---------- Lưu trữ dạng văn bản ----------
    private static String c(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').trim(); }

    public static String saveRoster(List<Student> list) {
        StringBuilder b = new StringBuilder();
        for (Student s : list)
            b.append(c(s.id)).append('\t').append(s.stt).append('\t').append(c(s.ho)).append('\t').append(c(s.ten))
             .append('\t').append(s.active ? 1 : 0).append('\n');
        return b.toString();
    }

    public static List<Student> loadRoster(String text) {
        List<Student> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length < 5) continue;
            Student s = new Student();
            s.id = p[0]; s.stt = Integer.parseInt(p[1]); s.ho = p[2]; s.ten = p[3]; s.active = p[4].equals("1");
            out.add(s);
        }
        return out;
    }

    public static String saveSessions(List<Session> list) {
        StringBuilder b = new StringBuilder();
        for (Session s : list)
            b.append(c(s.id)).append('\t').append(s.date).append('\t').append(c(s.buoi)).append('\t').append(c(s.gio))
             .append('\t').append(c(s.mon)).append('\t').append(c(s.phong)).append('\t').append(c(s.gv))
             .append('\t').append(s.checked ? 1 : 0).append('\t').append(String.join(",", s.absent)).append('\n');
        return b.toString();
    }

    public static List<Session> loadSessions(String text) {
        List<Session> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length < 9) continue;
            Session s = new Session();
            s.id = p[0]; s.date = LocalDate.parse(p[1]); s.buoi = p[2]; s.gio = p[3]; s.mon = p[4]; s.phong = p[5];
            s.gv = p[6]; s.checked = p[7].equals("1");
            for (String a : p[8].split(",")) if (!a.isEmpty()) s.absent.add(a);
            out.add(s);
        }
        return out;
    }

    /**
     * Gộp các buổi của tuần từ TKB vào danh sách buổi đã lưu. Buổi đã điểm danh giữ nguyên;
     * buổi chưa điểm danh được cập nhật môn/giờ/phòng; buổi bị bỏ khỏi lịch (từ hôm nay trở đi,
     * chưa điểm danh, nằm trong tuần đó) bị xóa.
     */
    public static List<Session> merge(List<Session> stored, Week w, LocalDate today) {
        if (w == null) return stored;
        Map<String, Session> map = new LinkedHashMap<>();
        for (Session s : stored) map.put(s.id, s);
        Set<String> inWeek = new HashSet<>();
        for (int i = 0; i < 7; i++)
            for (Week.Slot sl : w.days.get(i)) {
                LocalDate d = w.date(i);
                String id = sessionId(d, sl.buoi);
                inWeek.add(id);
                Session s = map.get(id);
                if (s == null) { s = new Session(); s.id = id; s.date = d; map.put(id, s); }
                if (s.checked) continue;
                s.buoi = sl.buoi; s.gio = sl.gio; s.mon = sl.mon; s.phong = sl.phong; s.gv = sl.gv;
            }
        LocalDate mon = w.monday, sun = w.monday.plusDays(6);
        map.values().removeIf(s -> !s.checked && !inWeek.contains(s.id) && !s.date.isBefore(mon)
                && !s.date.isAfter(sun) && !s.date.isBefore(today) && !s.id.contains("_them"));
        return new ArrayList<>(map.values());
    }
}
