package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.IteratorSupport;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Objects;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class J_U_Scanner {

    @Stub(value = JavaVersion.VERSION_1_9, include = {ScannerStream.class, IteratorSupport.class})
    public static Stream<String> tokens(Scanner scanner) {
        return new ScannerStream(scanner, null).tokens();
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = {ScannerStream.class, IteratorSupport.class})
    public static Stream<MatchResult> findAll(Scanner scanner, Pattern pattern) {
        return new ScannerStream(scanner, pattern).findAll();
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = {ScannerStream.class, IteratorSupport.class})
    public static Stream<MatchResult> findAll(Scanner scanner, String patternString) {
        return new ScannerStream(scanner, Pattern.compile(patternString)).findAll();
    }

    public static class ScannerStream {
        private final Scanner scanner;
        private final Pattern pattern;

        public ScannerStream(Scanner scanner, Pattern pattern) {
            this.scanner = scanner;
            this.pattern = pattern;
        }

        public boolean hasNextToken() {
            return scanner.hasNext();
        }

        public String nextToken() {
            return scanner.next();
        }

        public boolean hasNextMatch() {
            return scanner.findWithinHorizon(pattern, 0) != null;
        }

        public MatchResult nextMatch() {
            return scanner.match();
        }

        public Stream<String> tokens() {
            return new IteratorSupport<>(this::hasNextToken, this::nextToken).stream();
        }

        public Stream<MatchResult> findAll() {
            Objects.requireNonNull(pattern);
            return new IteratorSupport<>(this::hasNextMatch, this::nextMatch).stream();
        }

    }


}
