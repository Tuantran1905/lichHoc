package vn.lichhoc.cnt125;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;

/** Màu và hàm dựng giao diện dùng chung. */
final class Ui {
    static final int BG = Color.parseColor("#F3F5F4"), CARD = Color.WHITE, FG = Color.parseColor("#17211F"),
            MUTED = Color.parseColor("#5D6B68"), LINE = Color.parseColor("#DDE3E1"), ACCENT = Color.parseColor("#0F766E"),
            ACCENT_SOFT = Color.parseColor("#D8EFEB"), OK = Color.parseColor("#15803D"), OK_SOFT = Color.parseColor("#DCFCE7"),
            WARN = Color.parseColor("#B45309"), WARN_SOFT = Color.parseColor("#FEF3C7"), BAD = Color.parseColor("#B91C1C"),
            BAD_SOFT = Color.parseColor("#FEE2E2");
    static final String[] THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

    private Ui() { }

    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    static GradientDrawable round(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static LinearLayout card(Context c, boolean highlight) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = round(c, CARD, 12);
        bg.setStroke(dp(c, highlight ? 2 : 1), highlight ? ACCENT : LINE);
        card.setBackground(bg);
        card.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return card;
    }

    static TextView pill(Context c, String s, int fg, int bg) {
        TextView t = text(c, s, 12, fg, true);
        t.setBackground(round(c, bg, 99));
        t.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        return t;
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : FG);
        GradientDrawable bg = round(c, primary ? ACCENT : CARD, 10);
        if (!primary) bg.setStroke(dp(c, 1), LINE);
        b.setBackground(bg);
        b.setMinHeight(dp(c, 40));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        return b;
    }

    static LinearLayout.LayoutParams lp(Context c, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(c, topDp);
        return l;
    }

    static LinearLayout.LayoutParams wrap(Context c, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = dp(c, topDp);
        return l;
    }

    static String day(LocalDate d) {
        return THU[d.getDayOfWeek().getValue() - 1] + " " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
    }
}
