package vn.lichhoc.cnt125;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bảng màu, kiểu chữ và các khối giao diện dùng chung. */
final class Ui {
    // Nền và chữ
    static final int BG = c("#F2F4F7"), CARD = Color.WHITE, FG = c("#101828"), MUTED = c("#667085"),
            LINE = c("#E4E7EC"), BRAND = c("#0B5E57"), BRAND_DARK = c("#08433E"), BRAND_SOFT = c("#DDF1EE");
    // Trạng thái
    static final int OK = c("#067647"), OK_SOFT = c("#DCFAE6"), WARN = c("#B54708"), WARN_SOFT = c("#FEF0C7"),
            BAD = c("#B42318"), BAD_SOFT = c("#FEE4E2");
    static final String[] THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

    /** Màu riêng cho từng buổi để nhìn là biết học ngày hay đêm. */
    static final class Buoi {
        final String icon, label; final int bg, fg, sub, accent;
        Buoi(String icon, String label, int bg, int fg, int sub, int accent) {
            this.icon = icon; this.label = label; this.bg = bg; this.fg = fg; this.sub = sub; this.accent = accent;
        }
    }
    static final Buoi SANG = new Buoi("☀", "SÁNG", c("#FFF6DB"), c("#3D2A00"), c("#8A6100"), c("#F5A300"));
    static final Buoi CHIEU = new Buoi("⛅", "CHIỀU", c("#E3F1FF"), c("#0B2A4A"), c("#2E5A86"), c("#2E90FA"));
    static final Buoi TOI = new Buoi("☾", "TỐI", c("#1E1B4B"), c("#F5F3FF"), c("#C7C3F5"), c("#A5A1FF"));

    static Buoi buoi(String b) {
        if ("Sáng".equals(b)) return SANG;
        if ("Tối".equals(b)) return TOI;
        return CHIEU;
    }

    private Ui() { }

    private static int c(String hex) { return Color.parseColor(hex); }

    static int dp(Context ctx, int v) { return Math.round(v * ctx.getResources().getDisplayMetrics().density); }

    static TextView text(Context ctx, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    static TextView label(Context ctx, String s, int color) {
        TextView t = text(ctx, s.toUpperCase(), 11, color, true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    static GradientDrawable round(Context ctx, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(ctx, radiusDp));
        return g;
    }

    static LinearLayout vbox(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout card(Context ctx, boolean highlight) {
        LinearLayout card = vbox(ctx);
        GradientDrawable bg = round(ctx, CARD, 16);
        bg.setStroke(dp(ctx, highlight ? 2 : 1), highlight ? BRAND : LINE);
        card.setBackground(bg);
        card.setPadding(dp(ctx, 14), dp(ctx, 14), dp(ctx, 14), dp(ctx, 14));
        card.setElevation(dp(ctx, highlight ? 3 : 1));
        return card;
    }

    static TextView pill(Context ctx, String s, int fg, int bg) {
        TextView t = text(ctx, s, 12, fg, true);
        t.setBackground(round(ctx, bg, 99));
        t.setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4));
        return t;
    }

    static Button button(Context ctx, String s, boolean primary) {
        Button b = new Button(ctx);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(primary ? Color.WHITE : FG);
        GradientDrawable bg = round(ctx, primary ? BRAND : CARD, 12);
        if (!primary) bg.setStroke(dp(ctx, 1), LINE);
        b.setBackground(bg);
        b.setMinHeight(dp(ctx, 44));
        b.setMinimumHeight(dp(ctx, 44));
        b.setPadding(dp(ctx, 16), 0, dp(ctx, 16), 0);
        b.setStateListAnimator(null);
        return b;
    }

    static LinearLayout.LayoutParams lp(Context ctx, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(ctx, topDp);
        return l;
    }

    static LinearLayout.LayoutParams wrap(Context ctx, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = dp(ctx, topDp);
        return l;
    }

    static LinearLayout.LayoutParams weight(int w) { return new LinearLayout.LayoutParams(0, -2, w); }

    static LinearLayout.LayoutParams gapLeft(Context ctx, int leftDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.leftMargin = dp(ctx, leftDp);
        return l;
    }

    static String day(LocalDate d) {
        return THU[d.getDayOfWeek().getValue() - 1] + " " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
    }

    /** Tiêu đề ngày: "Thứ 4" lớn + ngày nhỏ + nhãn Hôm nay. */
    static LinearLayout dayHeader(Context ctx, LocalDate d, boolean today, String right) {
        LinearLayout h = hbox(ctx);
        TextView thu = text(ctx, THU[d.getDayOfWeek().getValue() - 1], 17, today ? BRAND : FG, true);
        h.addView(thu);
        TextView date = text(ctx, "  " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue()), 14, MUTED, false);
        h.addView(date, weight(1));
        if (today) h.addView(pill(ctx, "Hôm nay", Color.WHITE, BRAND));
        else if (right != null) h.addView(text(ctx, right, 12, MUTED, false));
        return h;
    }

    private static final Pattern TIET = Pattern.compile("\\((\\d+)\\s*/\\s*(\\d+)\\)");

    /**
     * Khối một buổi học, tô màu theo buổi: Sáng (vàng, ☀), Chiều (xanh, ⛅), Tối (tím đậm, ☾).
     * Có thanh tiến độ số tiết khi ô tiết dạng "(54/90)".
     */
    static LinearLayout slot(Context ctx, String buoi, String gio, String mon, String phong, String gv, String tiet) {
        Buoi st = buoi(buoi);
        LinearLayout row = hbox(ctx);
        row.setBackground(round(ctx, st.bg, 14));
        row.setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        row.setGravity(Gravity.TOP);

        LinearLayout time = vbox(ctx);
        time.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView icon = text(ctx, st.icon, 22, st.accent, false);
        icon.setGravity(Gravity.CENTER);
        time.addView(icon);
        TextView lb = text(ctx, st.label, 10, st.sub, true);
        lb.setLetterSpacing(0.1f);
        lb.setGravity(Gravity.CENTER);
        time.addView(lb);
        TextView g = text(ctx, gio, 15, st.fg, true);
        g.setGravity(Gravity.CENTER);
        g.setFontFeatureSettings("tnum");
        time.addView(g, wrap(ctx, 2));
        row.addView(time, new LinearLayout.LayoutParams(dp(ctx, 58), -2));

        View bar = new View(ctx);
        bar.setBackground(round(ctx, st.accent, 2));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(dp(ctx, 3), -1);
        bl.leftMargin = dp(ctx, 8);
        bl.rightMargin = dp(ctx, 12);
        row.addView(bar, bl);

        LinearLayout info = vbox(ctx);
        info.addView(text(ctx, mon, 16, st.fg, true));
        LinearLayout meta = hbox(ctx);
        if (phong != null && !phong.isEmpty()) {
            TextView room = text(ctx, "📍 " + phong, 13, st.fg, true);
            meta.addView(room);
        }
        if (gv != null && !gv.isEmpty()) {
            TextView t = text(ctx, (phong == null || phong.isEmpty() ? "" : "   ") + gv, 13, st.sub, false);
            meta.addView(t);
        }
        info.addView(meta, lp(ctx, 4));

        Matcher m = tiet == null ? null : TIET.matcher(tiet);
        if (m != null && m.find()) {
            int done = Integer.parseInt(m.group(1)), total = Math.max(1, Integer.parseInt(m.group(2)));
            LinearLayout prog = hbox(ctx);
            LinearLayout track = hbox(ctx);
            track.setBackground(round(ctx, st == TOI ? c("#3B3770") : c("#FFFFFF"), 4));
            View fill = new View(ctx);
            fill.setBackground(round(ctx, st.accent, 4));
            int pct = Math.min(100, Math.round(done * 100f / total));
            track.addView(fill, new LinearLayout.LayoutParams(0, dp(ctx, 5), Math.max(1, pct)));
            View rest = new View(ctx);
            track.addView(rest, new LinearLayout.LayoutParams(0, dp(ctx, 5), Math.max(0, 100 - pct)));
            prog.addView(track, weight(1));
            TextView pt = text(ctx, "  " + done + "/" + total + " tiết", 11, st.sub, false);
            pt.setFontFeatureSettings("tnum");
            prog.addView(pt);
            info.addView(prog, lp(ctx, 8));
        } else if (tiet != null && !tiet.isEmpty()) {
            info.addView(text(ctx, tiet, 12, st.sub, false), lp(ctx, 4));
        }
        row.addView(info, weight(1));
        return row;
    }

    /** Thanh tiêu đề màu thương hiệu ở đầu màn hình. */
    static LinearLayout appBar(Context ctx, String title, String sub) {
        LinearLayout bar = vbox(ctx);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BRAND, BRAND_DARK});
        bar.setBackground(g);
        bar.setPadding(dp(ctx, 20), dp(ctx, 18), dp(ctx, 20), dp(ctx, 20));
        TextView s = text(ctx, sub.toUpperCase(), 11, c("#B7E4DD"), true);
        s.setLetterSpacing(0.1f);
        bar.addView(s);
        bar.addView(text(ctx, title, 24, Color.WHITE, true), lp(ctx, 2));
        return bar;
    }
}
