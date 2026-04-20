package co.istad;

import co.istad.dto.MovieResponse;
import co.istad.model.Genre;
import co.istad.service.MovieService;
import co.istad.service.MovieServiceImpl;
import co.istad.utils.TableRenderer;

import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Main {

    // ── ANSI colour helpers ──────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String PURPLE = "\u001B[35m";
    private static final String DIM    = "\u001B[2m";
    private static final String BOLD   = "\u001B[1m";

    private static String g(String s)  { return GREEN  + s + RESET; }
    private static String c(String s)  { return CYAN   + s + RESET; }
    private static String p(String s)  { return PURPLE + s + RESET; }
    private static String dim(String s){ return DIM    + s + RESET; }
    private static String b(String s)  { return BOLD   + s + RESET; }

    // ── Scanner shared across all methods ───────────────────────────────────
    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean loading = false;

    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        MovieService movieService = new MovieServiceImpl();

        printBanner();

        while (true) {
            printMainMenu();
            String opt = prompt("Enter option").toLowerCase();

            switch (opt) {
                case "1" -> {
                    System.out.println(c("\n── Search by title ─────────────────────────────────"));

                    String title = prompt("Title");

                    Thread loader = startLoading("Searching movies");

                    MovieResponse response;
                    try {
                        response = movieService.getMoviesByTitleFromServer(1, title);
                    } finally {
                        stopLoading();
                    }

                    try {
                        loader.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    runBrowseLoop(
                            movieService,
                            page -> movieService.getMoviesByTitleFromServer(page, title)
                    );
                }
                case "2" -> {
                    System.out.println(c("\n--- Popular movies ----------------------------"));

                    Thread loader = startLoading("Loading popular movies");

                    try {
                        // first fetch only
                        movieService.getPopularMovies(1);
                    } finally {
                        stopLoading();
                    }

                    try {
                        loader.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    runBrowseLoop(movieService, movieService::getPopularMovies);
                }
                case "3" -> {
                    System.out.println(c("\n--- Browse by genre --------------------------"));

                    Thread loader = startLoading("Loading genres");

                    List<Genre> genres;
                    try {
                        genres = movieService.getGenres();
                    } finally {
                        stopLoading();
                    }

                    try {
                        loader.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    Genre selected = pickGenre(genres);
                    if (selected == null) break;

                    String genreId = String.valueOf(selected.getId());

                    runBrowseLoop(
                            movieService,
                            page -> movieService.getMoviesByGenre(page, genreId)
                    );
                }
                case "4" -> {
                    System.out.println(c("\n--- Top rated movies --------------------------"));
                    Thread loader = startLoading("Loading top rated movies");
                    try {
                        movieService.getTopRatedMovies(1);
                    } finally {
                        stopLoading();
                    }
                    try { loader.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    runBrowseLoop(movieService, movieService::getTopRatedMovies);
                }
                case "5" -> {
                    System.out.println(c("\n--- Upcoming movies ---------------------------"));
                    Thread loader = startLoading("Loading upcoming movies");
                    try {
                        movieService.getUpcomingMovies(1);
                    } finally {
                        stopLoading();
                    }
                    try { loader.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    runBrowseLoop(movieService, movieService::getUpcomingMovies);
                }
                case "e" -> exit();
                default  -> System.out.println(g("  Invalid option — try again.\n"));
            }
        }
    }


// Loading page
    private static Thread startLoading(String message) {
        loading = true;

        Thread t = new Thread(() -> {
            String[] frames = {"|", "/", "-", "\\"};
            int i = 0;

            while (loading) {
                System.out.print("\r" + c(message + " " + frames[i % frames.length]));
                i++;

                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {}
            }

            System.out.print("\r" + " ".repeat(message.length() + 5) + "\r");
        });

        t.start();
        return t;
    }

    private static void stopLoading() {
        loading = false;
    }


    // Core pagination loop — shared by all three browse modes

    private static void runBrowseLoop(
            MovieService movieService,
            Function<Integer, MovieResponse> fetcher) {

        int currentPage = 1;

        while (true) {

            MovieResponse response;
            try {
                response = fetcher.apply(currentPage);
            } catch (RuntimeException e) {
                System.out.println(g("  Error: " + e.getMessage() + "\n"));
                return;
            }

            TableRenderer.displayTableMoviesByTitle(response);
            printPageInfo(response);
            printNavMenu();

            String choice = prompt("Choose option").toLowerCase();

            switch (choice) {
                case "n"  -> currentPage = clampPage(response, currentPage + 1);
                case "p"  -> currentPage = clampPage(response, currentPage - 1);
                case "g"  -> {
                    try {
                        int target = Integer.parseInt(prompt("Go to page"));
                        currentPage = clampPage(response, target);
                    } catch (NumberFormatException e) {
                        System.out.println(g("  Not a valid number.\n"));
                    }
                }
                case "m" -> showMovieDetail(movieService);
                case "b"  -> { return; }
                case "e"  -> exit();
                default   -> System.out.println(g("  Invalid option.\n"));
            }
        }
    }


    // Movie detail view

    private static void showMovieDetail(MovieService movieService) {
        String movieId = prompt("Movie ID");
        try {
            TableRenderer.displayTableMovieDetails(
                    movieService.getMovieDetailsByTitleFromServer(movieId),
                    movieService.getMovieCaster(movieId)
            );
            System.out.println(p("  [b]") + " Back   " + p("[e]") + " Exit");
            if (prompt("Option").equalsIgnoreCase("e")) exit();
        } catch (RuntimeException e) {
            System.out.println(g("  Invalid ID.\n"));
        }
    }

    // Genre picker

    private static Genre pickGenre(List<Genre> genres) {
        for (int i = 0; i < genres.size(); i++) {
            System.out.printf("  %s  %s%n",
                    p(String.format("[%2d]", i + 1)),
                    genres.get(i).getName());
        }
        System.out.println();

        while (true) {
            String input = prompt("Choose genre");
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= genres.size()) {
                    return genres.get(choice - 1);
                }
                System.out.printf(g("  Enter a number between 1 and %d.%n%n"), genres.size());
            } catch (NumberFormatException e) {
                System.out.println(g("  Enter a valid number.\n"));
            }
        }
    }


    // Helpers

    private static int clampPage(MovieResponse r, int requested) {
        int max = Math.min(r.getTotal_pages(), 500);
        return Math.max(1, Math.min(requested, max));
    }

    private static String prompt(String label) {
        System.out.print(g("→ ") + b(label) + ": ");
        return scanner.nextLine().trim();
    }

    private static void exit() {
        System.out.println(dim("\n  Goodbye.\n"));
        System.exit(0);
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI printers
    // ════════════════════════════════════════════════════════════════════════
    private static void printBanner() {
        System.out.println(  """
 
  |---------------------------------------|
  |          Movie Search System          |
  |---------------------------------------|
""" );
    }
    private static void printMainMenu() {
        System.out.println(dim("-------------------------------------------------"));
        System.out.printf("|  %s  Search by title%n",   p("[ 1 ]"));
        System.out.printf("|  %s  Popular movies%n",    p("[ 2 ]"));
        System.out.printf("|  %s  Browse by genre%n",   p("[ 3 ]"));
        System.out.printf("|  %s  Top rated movies%n",  p("[ 4 ]"));
        System.out.printf("|  %s  Upcoming movies%n",   p("[ 5 ]"));  // ← new
        System.out.printf("|  %s  %s%n",                p("[ E ]"), dim("Exit"));
        System.out.println(dim("-------------------------------------------------"));
        System.out.println();
    }

    private static void printPageInfo(MovieResponse r) {
        System.out.printf("%n  Page %s / %s  ·  Results: %s%n",
                c(String.valueOf(r.getPage())),
                c(String.valueOf(r.getTotal_pages())),
                c(String.valueOf(r.getTotal_results())));
    }

    private static void printNavMenu() {
        System.out.println(dim("  ---------------------------------------------"));
        System.out.printf("|  %s %-16s %s %-16s%n", p("[n]"), "Next page",    p("[p]"), "Previous page");
        System.out.printf("|  %s %-16s %s %-16s%n", p("[g]"), "Go to page",   p("[b]"), "Back to menu");
        System.out.printf("|  %s %-16s %s %-16s%n", p("[m]"),"Movie detail", p("[e]"), "Exit");
        System.out.println(dim("  ---------------------------------------------"));
        System.out.println();
    }
}