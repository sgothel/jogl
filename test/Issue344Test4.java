public class Issue344Test4 extends Issue344Base {
    protected String getText() {
        // test 4 - unicode letter as second-to-last is rendered incorrectly
        return "\u201CGreetings\u201D!";
    }

    public static void main(String[] args) {
        new Issue344Test4().run(args);
    }
}
