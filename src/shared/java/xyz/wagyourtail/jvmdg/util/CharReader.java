package xyz.wagyourtail.jvmdg.util;


import java.util.Set;

public class CharReader {
    private final String buffer;
    private int pos = 0;

    public CharReader(String buf, int pos) {
        this.buffer = buf.replace("\r\n", "\n");
        this.pos = pos;
    }

    public CharReader copy() {
        return new CharReader(buffer, pos);
    }

    public boolean exhausted() {
        return pos >= buffer.length();
    }

    public int peek() {
        if (pos >= buffer.length()) return -1;
        return buffer.charAt(pos);
    }

    public int take() {
        if (pos >= buffer.length()) return -1;
        return buffer.charAt(pos++);
    }

    public String takeRemaining() {
        return takeUntil(-1);
    }

    public String takeLine() {
        return takeUntil('\n');
    }

    public String takeUntil(int c) {
        StringBuilder sb = new StringBuilder();
        while (pos < buffer.length()) {
            char ch = buffer.charAt(pos);
            if (ch == c) {
                break;
            }
            sb.append(ch);
            pos++;
        }
        return sb.toString();
    }

    public String takeUntil(Set<Integer> c) {
        StringBuilder sb = new StringBuilder();
        while (pos < buffer.length()) {
            char ch = buffer.charAt(pos);
            if (c.contains((int) ch)) {
                break;
            }
            sb.append(ch);
            pos++;
        }
        return sb.toString();
    }

    public String takeWhitespace() {
        StringBuilder sb = new StringBuilder();
        while (pos < buffer.length()) {
            char ch = buffer.charAt(pos);
            if (!Character.isWhitespace(ch)) {
                break;
            }
            pos++;
            sb.append(ch);
        }
        return sb.toString();
    }

    public String takeString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        int escapes = 0;
        while (pos < buffer.length()) {
            char c = (char) take();
            if (c == '"' && escapes == 0) {
                break;
            }
            if (c == '\\') {
                escapes++;
            } else {
                escapes = 0;
            }
            sb.append(c);
            if (escapes == 2) {
                escapes = 0;
            }
        }
        return sb.toString();
    }

    public void expect(char c) {
        int next = take();
        if (next != c) {
            throw new IllegalArgumentException("Expected " + c + ", found " + next);
        }
    }

}
