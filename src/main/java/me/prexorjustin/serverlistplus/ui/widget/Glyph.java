package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;

/**
 * Small pixel icons drawn with plain fills, so they look crisp at every GUI scale and need
 * neither textures nor font glyphs that some fonts lack.
 */
public enum Glyph {
    SEARCH(
        "..###....",
        ".#...#...",
        "#.....#..",
        "#.....#..",
        "#.....#..",
        ".#...#...",
        "..###.#..",
        ".......#.",
        "........#"),
    SETTINGS(
        "...#.#...",
        ".#.###.#.",
        "..#####..",
        ".##...##.",
        "###...###",
        ".##...##.",
        "..#####..",
        ".#.###.#.",
        "...#.#..."),
    PIN(
        ".###.",
        ".###.",
        ".###.",
        "#####",
        "..#..",
        "..#..",
        "..#.."),
    CLOCK(
        "..###..",
        ".#.#.#.",
        "#..#..#",
        "#..##.#",
        "#.....#",
        ".#...#.",
        "..###.."),
    CLOSE(
        "#.....#",
        ".#...#.",
        "..#.#..",
        "...#...",
        "..#.#..",
        ".#...#.",
        "#.....#"),
    NOTE(
        "#####..",
        "#...##.",
        "#.....#",
        "#.###.#",
        "#.....#",
        "#.###.#",
        "#######"),
    UPDATE(
        "....#....",
        "....#....",
        "....#....",
        "..#.#.#..",
        "...###...",
        "....#....",
        "#.......#",
        "#.......#",
        "#########");

    private final String[] rows;

    Glyph(String... rows) {
        this.rows = rows;
    }

    public int width() {
        return rows[0].length();
    }

    public int height() {
        return rows.length;
    }

    public void draw(Gfx gfx, int x, int y, int color) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') gfx.fill(x + column, y + row, x + column + 1, y + row + 1, color);
            }
        }
    }
}
