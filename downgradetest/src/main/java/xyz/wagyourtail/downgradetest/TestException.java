package xyz.wagyourtail.downgradetest;

public class TestException {

    public static void main(String[] args) {
        new Exception(new IndexOutOfBoundsException(1L).getMessage());
        try {
            throw new IndexOutOfBoundsException(1L);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e);
        }
    }

}
