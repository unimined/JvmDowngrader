package xyz.wagyourtail.downgradetest;

public class TestVersion {

    public static void main(String[] args) {
        System.out.println(Runtime.Version.parse("21.0.3+9-LTS"));
        System.out.println(Runtime.Version.parse("8.0.412+8"));

    }

}
