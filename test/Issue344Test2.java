public class Issue344Test2 extends Issue344Base {
    protected String getText() {
        // test 2 - unicode hangs program with a large font & long string
        return "\u201Cabcdefghijklmnopqrstuvwxyz\u201D";
    }

    public static void main(String[] args) {
        new Issue344Test2().run(args);
    }
}
