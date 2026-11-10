package vn.lichhoc.cnt125;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Lịch một tuần của lớp. Không phụ thuộc Android để có thể kiểm thử trên máy tính. */
public class Week {
    public static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final String[] THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

    public static class Slot {
        public String buoi = "", gio = "", mon = "", tiet = "", gv = "", phong = "";

        /** Nội dung dùng để so sánh thay đổi (bỏ qua số tiết tích lũy). */
        String compareKey() { return mon + "|" + gio + "|" + phong + "|" + gv; }

        public String shortText() {
            String s = mon;
            if (!phong.isEmpty()) s += " (" + phong + ")";
            return s;
        }
    }

    public String cvht = "";
    public String trangThai = "";
    public LocalDate monday;
    public final List<List<Slot>> days = new ArrayList<>();

    public Week() { for (int i = 0; i < 7; i++) days.add(new ArrayList<>()); }

    public LocalDate date(int i) { return monday.plusDays(i); }

    public String tuan() {
        return monday.format(DateTimeFormatter.ofPattern("dd/MM")) + " – " + monday.plusDays(6).format(DMY);
    }

    public String tuanNgan() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        return monday.format(f) + "–" + monday.plusDays(6).format(f);
    }

    public boolean isFinal() {
        return TkbParser.norm(trangThai).contains("HOANCHINH");
    }

    public String statusLabel() {
        if (trangThai.isEmpty()) return "";
        return isFinal() ? "Hoàn chỉnh" : (TkbParser.norm(trangThai).contains("XULY") ? "Đang xử lý" : cap(trangThai));
    }

    private static String cap(String s) {
        String l = s.toLowerCase();
        return l.isEmpty() ? l : Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }

    // ---------- Lưu trữ dạng văn bản đơn giản ----------
    private static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').trim(); }

    public String serialize() {
        StringBuilder b = new StringBuilder();
        b.append("H\t").append(monday.format(DMY)).append('\t').append(clean(cvht)).append('\t').append(clean(trangThai)).append('\n');
        for (int i = 0; i < 7; i++)
            for (Slot s : days.get(i))
                b.append("S\t").append(i).append('\t').append(clean(s.buoi)).append('\t').append(clean(s.gio)).append('\t')
                 .append(clean(s.mon)).append('\t').append(clean(s.tiet)).append('\t').append(clean(s.gv)).append('\t')
                 .append(clean(s.phong)).append('\n');
        return b.toString();
    }

    public static Week parse(String text) {
        if (text == null || text.isEmpty()) return null;
        Week w = new Week();
        for (String line : text.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p[0].equals("H") && p.length >= 4) {
                w.monday = LocalDate.parse(p[1], DMY);
                w.cvht = p[2];
                w.trangThai = p[3];
            } else if (p[0].equals("S") && p.length >= 8) {
                Slot s = new Slot();
                s.buoi = p[2]; s.gio = p[3]; s.mon = p[4]; s.tiet = p[5]; s.gv = p[6]; s.phong = p[7];
                w.days.get(Integer.parseInt(p[1])).add(s);
            }
        }
        return w.monday == null ? null : w;
    }

    // ---------- So sánh ----------
    /** Danh sách thay đổi nội dung (không tính trạng thái) giữa hai phiên bản cùng một tuần. */
    public static List<String> diff(Week oldW, Week newW) {
        List<String> out = new ArrayList<>();
        String[] buois = {"Sáng", "Chiều", "Tối"};
        for (int i = 0; i < 7; i++) {
            for (String b : buois) {
                Slot o = find(oldW.days.get(i), b), n = find(newW.days.get(i), b);
                String ok = o == null ? "" : o.compareKey(), nk = n == null ? "" : n.compareKey();
                if (ok.equals(nk)) continue;
                String before = o == null ? "Nghỉ" : o.shortText();
                String after = n == null ? "Nghỉ" : n.shortText() + (o != null && !o.gio.equals(n.gio) ? " lúc " + n.gio : "");
                if (o != null && n != null && o.mon.equals(n.mon) && o.phong.equals(n.phong) && !o.gv.equals(n.gv))
                    after = n.shortText() + " – " + n.gv;
                out.add(THU[i] + " " + b.toLowerCase() + ": " + before + " → " + after);
            }
        }
        return out;
    }

    private static Slot find(List<Slot> list, String buoi) {
        for (Slot s : list) if (s.buoi.equals(buoi)) return s;
        return null;
    }

    public String summary() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            b.append(THU[i]).append(": ");
            List<Slot> d = days.get(i);
            if (d.isEmpty()) b.append("Nghỉ");
            for (int k = 0; k < d.size(); k++) {
                Slot s = d.get(k);
                if (k > 0) b.append("; ");
                b.append(s.buoi).append(' ').append(s.gio).append(" ").append(s.shortText());
            }
            b.append('\n');
        }
        return b.toString().trim();
    }

    // ---------- Buổi học tiếp theo ----------
    public static class Upcoming {
        public Week week; public int day; public Slot slot; public LocalDateTime start; public boolean ongoing;
    }

    public LocalDateTime startOf(int day, Slot s) {
        int h = 7, m = 0;
        try {
            String[] p = s.gio.split(":");
            h = Integer.parseInt(p[0].trim());
            m = Integer.parseInt(p[1].trim());
        } catch (Exception ignored) { }
        return date(day).atTime(h, m);
    }

    /** Buổi đang học hoặc sắp tới gần nhất trong các tuần cho trước. */
    public static Upcoming next(LocalDateTime now, Week... weeks) {
        Upcoming best = null;
        for (Week w : weeks) {
            if (w == null) continue;
            for (int i = 0; i < 7; i++)
                for (Slot s : w.days.get(i)) {
                    LocalDateTime st = w.startOf(i, s);
                    LocalDateTime end = st.plusMinutes(s.tiet.contains("(4h)") ? 240 : 180);
                    if (end.isAfter(now) && (best == null || st.isBefore(best.start))) {
                        best = new Upcoming();
                        best.week = w; best.day = i; best.slot = s; best.start = st; best.ongoing = !st.isAfter(now);
                    }
                }
        }
        return best;
    }
}
