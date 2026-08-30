/**
 * Text blocks - strings glued with +, then the same strings written plainly.
 *
 * Run:  java TextBlockLab.java
 */

public class TextBlockLab {

    public static void main(String[] args) {

        // ---------- BEFORE ----------
        String sql = "SELECT p.id, p.name " +
                     "FROM product p" +
                     "JOIN category c ON c.id = p.category_id";

        String json = "{\"name\":\"Widget\",\"price\":9.99}";

        System.out.println("BEFORE - glued with +");
        System.out.println("  " + sql);
        System.out.println("     ^ one missing space, and you get 'product pJOIN'");
        System.out.println("  " + json + "     (every quote escaped in the source)");

        // ---------- AFTER ----------
        // The same two strings, written between """ and """.
        String blockSql = """
                SELECT p.id, p.name
                FROM product p
                JOIN category c ON c.id = p.category_id""";

        String blockJson = """
                {"name":"Widget","price":9.99}""";

        System.out.println();
        System.out.println("AFTER - text blocks");
        System.out.println(blockSql);
        System.out.println("  " + blockJson);
        System.out.println("  same String as the escaped one?  " + json.equals(blockJson));

        // ---------- the one rule ----------
        String withNewline = """
                hello
                """;
        String noNewline = """
                hello""";

        System.out.println();
        System.out.println("THE RULE - where the closing \"\"\" sits");
        System.out.println("  on its own line     [" + visible(withNewline) + "]");
        System.out.println("  on the text line    [" + visible(noNewline) + "]");
        System.out.println("  One character apart, and it is the one that fails your test.");
    }

    static String visible(String s) {
        return s.replace("\n", "\\n");
    }
}

/*
 * LIVE DEMO
 *
 *   Drag the closing """ of blockSql out to column 0 and run again: every line
 *   now carries sixteen leading spaces, and the output still looks almost
 *   right. That is the most common way students fail the text-block check in
 *   today's exercise. Undo afterwards.
 */
