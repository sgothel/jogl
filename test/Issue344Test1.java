public class Issue344Test1 extends Issue344Base {
    protected String getText() {
        // test 1 - weird artifacts appear with a large font & long string 
        return "abcdefghijklmnopqrstuvwxyz1234567890";
    }

    public static void main(String[] args) {
        new Issue344Test1().run(args);
    }
}
