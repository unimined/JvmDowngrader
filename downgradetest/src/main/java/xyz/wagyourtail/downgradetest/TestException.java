package xyz.wagyourtail.downgradetest;

public class TestException {

    public static void main(String[] args) {
        new Exception(new IndexOutOfBoundsException(1L).getMessage());
        try {
            throw new IndexOutOfBoundsException(1L);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e);
        }

        new VirtualMachineError("test", new IndexOutOfBoundsException(1L)) {};
        try {
            throw new VirtualMachineError("test", new IndexOutOfBoundsException(1L)) {};
        } catch (VirtualMachineError e) {
            System.out.println(e);
        }

        new InternalError("test", new IndexOutOfBoundsException(1L));
        try {
            throw new InternalError("test", new IndexOutOfBoundsException(1L));
        } catch (InternalError e) {
            System.out.println(e);
        }

    }

}
