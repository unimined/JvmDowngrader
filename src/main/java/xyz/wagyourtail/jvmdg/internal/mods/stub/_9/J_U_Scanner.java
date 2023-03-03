package xyz.wagyourtail.jvmdg.internal.mods.stub._9;

import org.gradle.api.JavaVersion;
import xyz.wagyourtail.jvmdg.internal.mods.stub.Stub;

import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class J_U_Scanner {

    @Stub(value = JavaVersion.VERSION_1_9, include = ScannerStream.class)
    public static Stream<String> tokens(Scanner scanner) {
        return new ScannerStream(scanner).tokens();
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = ScannerStream.class)
    public static Stream<MatchResult> findAll(Scanner scanner, Pattern pattern) {
        return new ScannerStream(scanner).findAll(pattern);
    }

    @Stub(value = JavaVersion.VERSION_1_9, include = ScannerStream.class)
    public static Stream<MatchResult> findAll(Scanner scanner, String patternString) {
        return new ScannerStream(scanner).findAll(patternString);
    }

    public static class ScannerStream {
        private final Scanner scanner;

        public ScannerStream(Scanner scanner) {
            this.scanner = scanner;
        }

        public boolean hasNextToken(String useless) {
            return scanner.hasNext();
        }

        public String nextToken() {
            return scanner.next();
        }

        public boolean hasNextMatch(Pattern pattern) {
            return scanner.findWithinHorizon(pattern, 0) != null;
        }

        public MatchResult nextMatch(Pattern pattern) {
            return scanner.match();
        }

        public Stream<String> tokens() {
            return Stream.generate(this::nextToken).takeWhile(this::hasNextToken).onClose(scanner::close);
        }

        public Stream<MatchResult> findAll(Pattern pattern) {
            Objects.requireNonNull(pattern);
            return Stream.iterate(pattern, UnaryOperator.identity()).takeWhile(this::hasNextMatch).map(this::nextMatch).onClose(scanner::close);
        }

        public Stream<MatchResult> findAll(String patternString) {
            Objects.requireNonNull(patternString);
            return this.findAll(Pattern.compile(patternString));
        }

    }


}
