package co.istad.utils;

import co.istad.dto.CasterResponse;
import co.istad.dto.MovieDetailResponse;
import co.istad.dto.MovieResponse;
import co.istad.model.Genre;
import co.istad.model.Movie;
import co.istad.model.Production;

import java.util.ArrayList;
import java.util.List;

public class TableRenderer {

    // ─── ANSI color codes ──────────────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";

    private static final String FG_WHITE   = "\u001B[97m";
    private static final String FG_YELLOW  = "\u001B[93m";
    private static final String FG_CYAN    = "\u001B[96m";
    private static final String FG_GREEN   = "\u001B[92m";
    private static final String FG_MAGENTA = "\u001B[95m";
    private static final String FG_BLUE    = "\u001B[94m";
    private static final String FG_GRAY    = "\u001B[90m";
    private static final String FG_RED     = "\u001B[91m";

    private static final String BG_DARK    = "\u001B[48;5;235m";
    private static final String BG_DARKER  = "\u001B[48;5;233m";
    private static final String BG_HEADER  = "\u001B[48;5;24m";   // deep blue
    private static final String BG_ALT     = "\u001B[48;5;236m";  // alternating row

    // ─── Box-drawing chars ────────────────────────────────────────────────────
    private static final String H  = "─";
    private static final String V  = "│";
    private static final String TL = "╭";
    private static final String TR = "╮";
    private static final String BL = "╰";
    private static final String BR = "╯";
    private static final String MT = "┬";
    private static final String MB = "┴";
    private static final String ML = "├";
    private static final String MR = "┤";
    private static final String MC = "┼";
    private static final String MH = "─";

    // Column widths for movie search results
    private static final int[] MOVIE_COLS = {6, 36, 14, 8, 46};
    private static final String[] MOVIE_HEADERS = {"ID", "TITLE", "RELEASED", "RATING", "TRAILER"};

    // Column widths for cast table
    private static final int[] CAST_COLS = {28, 10, 30};
    private static final String[] CAST_HEADERS = {"ACTOR", "GENDER", "CHARACTER"};

    // ─── Public API ───────────────────────────────────────────────────────────

    public static void displayTableMoviesByTitle(MovieResponse movieResponse) {
        int totalWidth = totalWidth(MOVIE_COLS);

        printSectionLabel("🎬  SEARCH RESULTS", totalWidth);
        printTopBorder(MOVIE_COLS);
        printHeaderRow(MOVIE_HEADERS, MOVIE_COLS, BG_HEADER);
        printDivider(MOVIE_COLS);

        boolean alt = false;
        for (Movie movie : movieResponse.getResults()) {
            String[] row = {
                    orNA(movie.getId()),
                    orNA(movie.getTitle()),
                    orNA(movie.getRelease_date()),
                    formatRating(movie.getVote_average()),
                    movie.getTrailer() != null
                            ? "https://youtu.be/" + movie.getTrailer().getKey()
                            : "N/A"
            };
            printDataRow(row, MOVIE_COLS, alt ? BG_ALT : BG_DARK);
            alt = !alt;
        }
        printBottomBorder(MOVIE_COLS);
        System.out.println();
    }

    public static void displayTableMovieDetails(MovieDetailResponse movie, CasterResponse casters) {
        int detailWidth = 66;
        printSectionLabel("🎥  MOVIE DETAILS", detailWidth);
        printMovieDetailCard(movie, detailWidth);
        System.out.println();

        int castWidth = totalWidth(CAST_COLS);
        printSectionLabel("🎭  CAST", castWidth);
        printTopBorder(CAST_COLS);
        printHeaderRow(CAST_HEADERS, CAST_COLS, BG_HEADER);
        printDivider(CAST_COLS);

        boolean alt = false;
        for (var c : casters.getCast()) {
            String gender = switch (c.getGender()) {
                case "1" -> "♀ Female";
                case "2" -> "♂ Male";
                default  -> "? Unknown";
            };
            String[] row = {
                    orNA(c.getName()),
                    gender,
                    orNA(c.getCharacter())
            };
            printDataRow(row, CAST_COLS, alt ? BG_ALT : BG_DARK);
            alt = !alt;
        }
        printBottomBorder(CAST_COLS);
        System.out.println();
    }

    // ─── Detail card ──────────────────────────────────────────────────────────

    private static void printMovieDetailCard(MovieDetailResponse m, int width) {
        // Gather genres / countries / studios
        List<String> genres = new ArrayList<>();
        for (Genre g : m.getGenres()) genres.add(g.getName());

        List<String> studios = new ArrayList<>();
        for (Production p : m.getProduction_companies()) studios.add(p.getName());

        // Title banner
        String titleLine = pad(" " + orNA(m.getTitle()), width - 2);
        System.out.println(TL + H.repeat(width - 2) + TR);
        System.out.println(V + BG_HEADER + BOLD + FG_WHITE + titleLine + RESET + V);
        System.out.println(ML + MH.repeat(width - 2) + MR);

        // Two-column metadata grid
        printDetailRow("ID",          orNA(m.getId()),                 "RUNTIME",  orNA(m.getRuntime()) + " min", width);
        printDetailRow("RELEASED",    orNA(m.getRelease_date()),       "RATING",   formatRating(m.getVote_average()), width);


        System.out.println(ML + MH.repeat(width - 2) + MR);

        // Full-width overview
        printFullRow("OVERVIEW", wordWrap(orNA(m.getOverview()), width - 18), width);

        System.out.println(ML + MH.repeat(width - 2) + MR);

        // Genres & studios
        printFullRow("GENRES",   joinList(genres),  width);
        printFullRow("STUDIOS",  joinList(studios), width);

        System.out.println(BL + H.repeat(width - 2) + BR);
    }

    // ─── Row printers ────────────────────────────────────────────────────────

    private static void printDetailRow(String k1, String v1, String k2, String v2, int width) {
        int half = (width - 3) / 2;
        String left  = DIM + FG_CYAN  + pad(" " + k1 + ": ", 12) + RESET + FG_WHITE + truncate(v1, half - 14);
        String right = DIM + FG_CYAN  + pad(" " + k2 + ": ", 12) + RESET + FG_WHITE + truncate(v2, half - 14);
        System.out.println(V + pad(stripAnsi(left)  < half ? left  + " ".repeat(half - visLen(left))  : left,  half) + V
                + pad(stripAnsi(right) < half ? right + " ".repeat(half - visLen(right)) : right, half + (width % 2)) + V);
    }

    private static void printFullRow(String label, String value, int width) {
        int innerW = width - 2;
        String header = DIM + FG_CYAN + " " + label + ": " + RESET;
        String content = FG_WHITE + truncate(value, innerW - label.length() - 4) + RESET;
        int used = 1 + label.length() + 2 + visLen(content);
        System.out.println(V + header + content + " ".repeat(Math.max(0, innerW - used)) + V);
    }

    private static void printTopBorder(int[] cols) {
        StringBuilder sb = new StringBuilder(TL);
        for (int i = 0; i < cols.length; i++) {
            sb.append(H.repeat(cols[i] + 2));
            sb.append(i < cols.length - 1 ? MT : TR);
        }
        System.out.println(sb);
    }

    private static void printBottomBorder(int[] cols) {
        StringBuilder sb = new StringBuilder(BL);
        for (int i = 0; i < cols.length; i++) {
            sb.append(H.repeat(cols[i] + 2));
            sb.append(i < cols.length - 1 ? MB : BR);
        }
        System.out.println(sb);
    }

    private static void printDivider(int[] cols) {
        StringBuilder sb = new StringBuilder(ML);
        for (int i = 0; i < cols.length; i++) {
            sb.append(MH.repeat(cols[i] + 2));
            sb.append(i < cols.length - 1 ? MC : MR);
        }
        System.out.println(sb);
    }

    private static void printHeaderRow(String[] headers, int[] cols, String bg) {
        StringBuilder sb = new StringBuilder();
        sb.append(V);
        for (int i = 0; i < headers.length; i++) {
            sb.append(bg).append(BOLD).append(FG_YELLOW)
                    .append(" ").append(pad(headers[i], cols[i])).append(" ")
                    .append(RESET).append(V);
        }
        System.out.println(sb);
    }

    private static void printDataRow(String[] values, int[] cols, String bg) {
        StringBuilder sb = new StringBuilder();
        sb.append(V);
        for (int i = 0; i < values.length; i++) {
            String v   = truncate(values[i], cols[i]);
            String colored = colorValue(i, v);
            int    pad = cols[i] - visLen(v);
            sb.append(bg).append(" ").append(colored).append(" ".repeat(Math.max(0, pad))).append(" ").append(RESET).append(V);
        }
        System.out.println(sb);
    }

    // ─── Formatting helpers ──────────────────────────────────────────────────

    /** Colorise column values for the movie table. */
    private static String colorValue(int col, String v) {
        return switch (col) {
            case 0 -> DIM + FG_GRAY  + v + RESET;
            case 1 -> BOLD + FG_WHITE + v + RESET;
            case 2 -> FG_CYAN        + v + RESET;
            case 3 -> ratingColor(v) + v + RESET;
            case 4 -> FG_BLUE        + v + RESET;
            default -> v;
        };
    }

    private static String ratingColor(String rating) {
        try {
            double r = Double.parseDouble(rating);
            if (r >= 8.0) return FG_GREEN;
            if (r >= 6.0) return FG_YELLOW;
            return FG_RED;
        } catch (NumberFormatException e) {
            return FG_GRAY;
        }
    }

    private static String formatRating(Double r) {
        return r == null ? "N/A" : String.format("%.1f", r);
    }

    private static String formatBudget(Long b) {
        if (b == null || b == 0) return "N/A";
        return String.format("$%,d", b);
    }

    private static String joinList(List<String> list) {
        return list == null || list.isEmpty() ? "N/A" : String.join(", ", list);
    }

    private static String orNA(Object o) {
        return (o == null || o.toString().isBlank()) ? "N/A" : o.toString();
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "N/A";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String wordWrap(String text, int width) {
        if (text.length() <= width) return text;
        int cut = text.lastIndexOf(' ', width);
        return cut > 0 ? text.substring(0, cut) + "…" : text.substring(0, width) + "…";
    }

    private static void printSectionLabel(String label, int width) {
        System.out.println();
        System.out.println(BOLD + FG_MAGENTA + label + RESET
                + DIM + FG_GRAY + "  " + H.repeat(Math.max(0, width - label.length() - 2)) + RESET);
    }

    /** Visual length of a string (strips ANSI escape codes). */
    private static int visLen(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "").length();
    }

    /** Returns raw length without ANSI — used only as a type cast helper. */
    private static int stripAnsi(String s) {
        return visLen(s);
    }

    private static int totalWidth(int[] cols) {
        int w = 1;
        for (int c : cols) w += c + 3;
        return w - 1;
    }
}