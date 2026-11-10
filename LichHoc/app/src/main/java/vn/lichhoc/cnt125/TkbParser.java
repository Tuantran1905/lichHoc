package vn.lichhoc.cnt125;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Đọc file thời khóa biểu .xlsx của Khoa Kinh tế - CNTT và lấy lịch của một lớp.
 * Bố cục mỗi sheet: cột A = TT, B = tên lớp / CVHT / trạng thái, C = buổi, D..J = Thứ 2..Chủ nhật.
 * Mỗi buổi gồm 4 dòng: môn, số tiết, giáo viên, phòng.
 */
public class TkbParser {
    private static final int COLS = 12;
    private static final Pattern SI = Pattern.compile("<si>(.*?)</si>|<si/>", Pattern.DOTALL);
    private static final Pattern RPH = Pattern.compile("<rPh\\b.*?</rPh>", Pattern.DOTALL);
    private static final Pattern T = Pattern.compile("<t(?:\\s[^>]*)?>(.*?)</t>", Pattern.DOTALL);
    private static final Pattern ROW = Pattern.compile("<row\\b([^>]*?)(?:/>|>(.*?)</row>)", Pattern.DOTALL);
    private static final Pattern ROW_R = Pattern.compile("\\br=\"(\\d+)\"");
    private static final Pattern CELL = Pattern.compile("<c\\b([^>]*?)(?:/>|>(.*?)</c>)", Pattern.DOTALL);
    private static final Pattern CELL_R = Pattern.compile("\\br=\"([A-Z]+)(\\d+)\"");
    private static final Pattern CELL_T = Pattern.compile("\\bt=\"(\\w+)\"");
    private static final Pattern V = Pattern.compile("<v>(.*?)</v>", Pattern.DOTALL);
    private static final Pattern DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})[:h](\\d{2})");

    public static String norm(String s) {
        if (s == null) return "";
        String t = s.replace('Đ', 'D').replace('đ', 'd');
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /** Trả về mọi tuần tìm thấy của lớp (mỗi sheet một tuần). */
    public static List<Week> parse(File xlsx, String className) throws IOException {
        String key = norm(className);
        List<Week> out = new ArrayList<>();
        try (ZipFile z = new ZipFile(xlsx)) {
            List<String> shared = new ArrayList<>();
            ZipEntry se = z.getEntry("xl/sharedStrings.xml");
            if (se != null) {
                String xml = read(z, se);
                Matcher m = SI.matcher(xml);
                while (m.find()) {
                    String inner = m.group(1) == null ? "" : RPH.matcher(m.group(1)).replaceAll("");
                    StringBuilder b = new StringBuilder();
                    Matcher t = T.matcher(inner);
                    while (t.find()) b.append(t.group(1));
                    shared.add(unescape(b.toString()));
                }
            }
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.getName().matches("xl/worksheets/sheet\\d+\\.xml")) continue;
                TreeMap<Integer, String[]> rows = readSheet(read(z, e), shared);
                Week w = findClass(rows, key);
                if (w != null) out.add(w);
            }
        }
        return out;
    }

    static TreeMap<Integer, String[]> readSheet(String xml, List<String> shared) {
        TreeMap<Integer, String[]> rows = new TreeMap<>();
        Matcher rm = ROW.matcher(xml);
        while (rm.find()) {
            Matcher rr = ROW_R.matcher(rm.group(1));
            if (!rr.find() || rm.group(2) == null) continue;
            int r = Integer.parseInt(rr.group(1));
            String[] cells = new String[COLS];
            Matcher cm = CELL.matcher(rm.group(2));
            while (cm.find()) {
                Matcher ref = CELL_R.matcher(cm.group(1));
                if (!ref.find()) continue;
                int col = colIndex(ref.group(1));
                if (col >= COLS) continue;
                String body = cm.group(2) == null ? "" : cm.group(2);
                Matcher tm = CELL_T.matcher(cm.group(1));
                String type = tm.find() ? tm.group(1) : "n";
                String val;
                if (type.equals("inlineStr")) {
                    StringBuilder b = new StringBuilder();
                    Matcher t = T.matcher(body);
                    while (t.find()) b.append(t.group(1));
                    val = unescape(b.toString());
                } else {
                    Matcher v = V.matcher(body);
                    val = v.find() ? unescape(v.group(1)) : "";
                    if (type.equals("s") && !val.isEmpty()) {
                        int idx = Integer.parseInt(val.trim());
                        val = idx < shared.size() ? shared.get(idx) : "";
                    }
                }
                cells[col] = val.replace('\n', ' ').replaceAll("\\s+", " ").trim();
            }
            rows.put(r, cells);
        }
        return rows;
    }

    static Week findClass(TreeMap<Integer, String[]> rows, String key) {
        List<Integer> nums = new ArrayList<>(rows.keySet());
        String sheetStatus = "";
        for (int i = 0; i < nums.size(); i++) {
            String[] row = rows.get(nums.get(i));
            String b = cell(row, 1);
            if (sheetStatus.isEmpty() && isStatus(b) && i < 12) sheetStatus = b;
            if (!norm(b).equals(key) || i + 1 >= nums.size()) continue;

            Week w = new Week();
            String[] dateRow = rows.get(nums.get(i + 1));
            for (int d = 0; d < 7; d++) {
                LocalDate dt = parseDate(cell(dateRow, 3 + d));
                if (dt != null) { w.monday = dt.minusDays(d); break; }
            }
            if (w.monday == null) return null;

            List<String[]> block = new ArrayList<>();
            for (int k = i + 2; k < nums.size() && k < i + 22; k++) {
                String[] r = rows.get(nums.get(k));
                String a = cell(r, 0), bb = cell(r, 1);
                if (a.matches("\\d+") || norm(bb).matches("TA(CD|TC|LTCD)[A-Z]+\\d+")
                        || norm(cell(r, 3)).contains("BANGIAMHIEU")) break;
                block.add(r);
            }

            for (int k = 0; k < block.size(); k++) {
                String bb = cell(block.get(k), 1);
                if (isStatus(bb) && w.trangThai.isEmpty()) w.trangThai = bb.toUpperCase();
                String nb = norm(bb);
                if (w.cvht.isEmpty() && (nb.startsWith("CVHT") || nb.startsWith("GVCN"))) {
                    int c = bb.indexOf(':');
                    if (c >= 0 && bb.substring(c + 1).trim().length() > 2) w.cvht = bb.substring(c + 1).trim();
                    else if (k + 1 < block.size()) {
                        String nx = cell(block.get(k + 1), 1);
                        if (!nx.isEmpty() && !isStatus(nx)) w.cvht = nx;
                    }
                }
            }
            if (w.trangThai.isEmpty()) w.trangThai = sheetStatus.toUpperCase();

            for (int k = 0; k < block.size(); k++) {
                String buoi = sessionName(cell(block.get(k), 2));
                if (buoi == null) continue;
                String[][] g = new String[4][];
                for (int j = 0; j < 4; j++) {
                    boolean inside = k + j < block.size() && (j == 0 || sessionName(cell(block.get(k + j), 2)) == null);
                    g[j] = inside ? block.get(k + j) : new String[COLS];
                }
                String defTime = buoi.equals("Sáng") ? "7:00" : buoi.equals("Chiều") ? "13:00" : "17:30";
                String rowTime = findTime(cell(g[1], 2));
                if (rowTime != null) defTime = rowTime;
                for (int d = 0; d < 7; d++) {
                    String mon = cell(g[0], 3 + d);
                    if (mon.isEmpty()) continue;
                    Week.Slot s = new Week.Slot();
                    s.buoi = buoi;
                    s.mon = mon;
                    s.tiet = cell(g[1], 3 + d);
                    s.gv = cell(g[2], 3 + d);
                    s.phong = cell(g[3], 3 + d);
                    String t = findTime(s.tiet);
                    if (t == null) t = findTime(s.mon);
                    s.gio = t != null ? t : defTime;
                    w.days.get(d).add(s);
                }
            }
            return w;
        }
        return null;
    }

    static String sessionName(String c) {
        String n = norm(c);
        if (n.equals("SANG")) return "Sáng";
        if (n.equals("CHIEU")) return "Chiều";
        if (n.equals("TOI")) return "Tối";
        return null;
    }

    static boolean isStatus(String s) {
        String n = norm(s);
        return n.equals("HOANCHINH") || n.equals("XULY") || n.contains("DIEUCHINH") && n.length() < 16;
    }

    static String findTime(String s) {
        if (s == null) return null;
        Matcher m = TIME.matcher(s);
        if (!m.find()) return null;
        int h = Integer.parseInt(m.group(1)), mi = Integer.parseInt(m.group(2));
        if (h > 23 || mi > 59) return null;
        return h + ":" + (mi < 10 ? "0" : "") + mi;
    }

    static LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        Matcher m = DATE.matcher(s);
        try {
            if (m.find())
                return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            double serial = Double.parseDouble(s.trim());
            if (serial > 36000 && serial < 80000) return LocalDate.of(1899, 12, 30).plusDays((long) serial);
        } catch (Exception ignored) { }
        return null;
    }

    private static String cell(String[] row, int i) {
        return row == null || i >= row.length || row[i] == null ? "" : row[i];
    }

    private static int colIndex(String letters) {
        int n = 0;
        for (char ch : letters.toCharArray()) n = n * 26 + (ch - 'A' + 1);
        return n - 1;
    }

    private static String read(ZipFile z, ZipEntry e) throws IOException {
        try (InputStream in = z.getInputStream(e)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String unescape(String s) {
        if (s.indexOf('&') < 0) return s;
        StringBuilder b = new StringBuilder();
        Matcher m = Pattern.compile("&(#x?[0-9a-fA-F]+|amp|lt|gt|quot|apos);").matcher(s);
        int last = 0;
        while (m.find()) {
            b.append(s, last, m.start());
            String g = m.group(1);
            if (g.startsWith("#x")) b.appendCodePoint(Integer.parseInt(g.substring(2), 16));
            else if (g.startsWith("#")) b.appendCodePoint(Integer.parseInt(g.substring(1)));
            else b.append(g.equals("amp") ? "&" : g.equals("lt") ? "<" : g.equals("gt") ? ">" : g.equals("quot") ? "\"" : "'");
            last = m.end();
        }
        return b.append(s.substring(last)).toString();
    }
}
