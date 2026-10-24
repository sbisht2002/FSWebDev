package com.fswebdev.fit90.content;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fswebdev.fit90.ui.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the small markdown subset used in /content: #/##/### headings, paragraphs,
 * - and 1. lists, > quotes, --- rules, **bold**, *italic*, `code` and [links](url).
 * Links open in the browser (or call {@link LinkHandler} for in-app "app:" links).
 */
public final class Markdown {

    public interface LinkHandler {
        /** Return true if the link was handled inside the app. */
        boolean open(String url);
    }

    private static final Pattern INLINE = Pattern.compile(
            "\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|_(.+?)_|`(.+?)`|\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");

    private Markdown() {
    }

    public static String stripFrontMatter(String md) {
        if (md.startsWith("---")) {
            int end = md.indexOf("\n---", 3);
            if (end > 0) {
                int nl = md.indexOf('\n', end + 4);
                return nl < 0 ? "" : md.substring(nl + 1);
            }
        }
        return md;
    }

    /** Front-matter values as raw strings (lists stay as "[a, b]"). */
    public static java.util.Map<String, String> frontMatter(String md) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        if (!md.startsWith("---")) return out;
        int end = md.indexOf("\n---", 3);
        if (end < 0) return out;
        for (String line : md.substring(3, end).split("\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String v = line.substring(colon + 1).trim();
            if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\""))) v = v.substring(1, v.length() - 1);
            out.put(line.substring(0, colon).trim(), v);
        }
        return out;
    }

    /** Inline formatting for one block of text. */
    public static CharSequence inline(String text, final LinkHandler links) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher m = INLINE.matcher(text);
        int last = 0;
        while (m.find()) {
            sb.append(text, last, m.start());
            int start = sb.length();
            if (m.group(1) != null) {
                sb.append(m.group(1));
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m.group(2) != null || m.group(3) != null) {
                sb.append(m.group(2) != null ? m.group(2) : m.group(3));
                sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m.group(4) != null) {
                sb.append(m.group(4));
                sb.setSpan(new TypefaceSpan("monospace"), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.append(m.group(5));
                final String url = m.group(6);
                sb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (links != null && links.open(url)) return;
                        try {
                            widget.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        } catch (Exception ignored) {
                            // No browser installed.
                        }
                    }
                }, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            last = m.end();
        }
        sb.append(text, last, text.length());
        return sb;
    }

    /** Plain text without markdown markers (for search and text-to-speech). */
    public static String plain(String md) {
        String s = stripFrontMatter(md);
        s = s.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        s = s.replaceAll("[*_`#>]", "");
        return s;
    }

    private static final class Block {
        final char kind; // h, p, u, o, q, r
        final int level;
        final String text;

        Block(char kind, int level, String text) {
            this.kind = kind;
            this.level = level;
            this.text = text;
        }
    }

    static List<Block> parse(String md) {
        List<Block> blocks = new ArrayList<Block>();
        StringBuilder para = new StringBuilder();
        char paraKind = 'p';
        int number = 0;
        for (String raw : stripFrontMatter(md).split("\n")) {
            String line = raw.trim();
            boolean bullet = line.startsWith("- ") || line.startsWith("* ");
            boolean numbered = line.matches("^\\d+[.)] .*");
            boolean quote = line.startsWith(">");
            if (line.isEmpty() || line.startsWith("#") || line.equals("---") || bullet || numbered
                    || (quote && paraKind != 'q') || (!quote && paraKind == 'q')) {
                if (para.length() > 0) blocks.add(new Block(paraKind, 0, para.toString().trim()));
                para.setLength(0);
                paraKind = 'p';
            }
            if (line.isEmpty()) {
                number = 0;
                continue;
            }
            if (line.equals("---")) {
                blocks.add(new Block('r', 0, ""));
            } else if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') level++;
                blocks.add(new Block('h', level, line.substring(level).trim()));
            } else if (bullet) {
                blocks.add(new Block('u', 0, line.substring(2).trim()));
            } else if (numbered) {
                number++;
                blocks.add(new Block('o', number, line.replaceFirst("^\\d+[.)] ", "")));
            } else if (quote) {
                paraKind = 'q';
                para.append(line.replaceFirst("^>\\s?", "")).append(' ');
            } else {
                para.append(line).append(' ');
            }
        }
        if (para.length() > 0) blocks.add(new Block(paraKind, 0, para.toString().trim()));
        return blocks;
    }

    /** Appends views for {@code md} to {@code into}. */
    public static void render(Ui ui, String md, LinearLayout into, LinkHandler links) {
        for (Block b : parse(md)) {
            TextView t;
            switch (b.kind) {
                case 'h':
                    t = ui.text(inline(b.text, links), b.level <= 1 ? Ui.TITLE : b.level == 2 ? Ui.HEADING : Ui.SUBHEAD);
                    t.setPadding(0, ui.dp(b.level <= 2 ? 18 : 12), 0, ui.dp(6));
                    break;
                case 'u':
                case 'o': {
                    LinearLayout row = ui.row();
                    row.setGravity(android.view.Gravity.TOP);
                    TextView mark = ui.text(b.kind == 'u' ? "•" : b.level + ".", Ui.BODY, ui.p.accent);
                    mark.setMinWidth(ui.dp(22));
                    row.addView(mark);
                    TextView body = ui.text(inline(b.text, links), Ui.BODY);
                    body.setMovementMethod(LinkMovementMethod.getInstance());
                    row.addView(body, ui.weight(1));
                    row.setPadding(ui.dp(4), ui.dp(3), 0, ui.dp(3));
                    into.addView(row);
                    continue;
                }
                case 'q':
                    t = ui.text(inline(b.text, links), Ui.BODY);
                    t.setBackground(ui.round(ui.p.cardAlt, 14));
                    t.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
                    t.setLayoutParams(ui.margins(ui.fill(), 0, 8, 0, 8));
                    break;
                case 'r':
                    View d = ui.divider();
                    d.setLayoutParams(ui.margins(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, ui.dp(1))), 0, 14, 0, 14));
                    into.addView(d);
                    continue;
                default:
                    t = ui.text(inline(b.text, links), Ui.BODY);
                    t.setPadding(0, ui.dp(4), 0, ui.dp(8));
            }
            t.setMovementMethod(LinkMovementMethod.getInstance());
            into.addView(t);
        }
    }
}
