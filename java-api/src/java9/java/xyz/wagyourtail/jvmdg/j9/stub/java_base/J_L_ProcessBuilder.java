package xyz.wagyourtail.jvmdg.j9.stub.java_base;

import xyz.wagyourtail.jvmdg.version.Ref;
import xyz.wagyourtail.jvmdg.version.Stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class J_L_ProcessBuilder {

    @Stub(ref = @Ref("java/lang/ProcessBuilder"))
    public static List<Process> startPipeline(List<ProcessBuilder> builders) throws IOException {
        List<Process> processes = new ArrayList<>();
        try {
            InputStream prevOutput = null;
            for (int i = 0; i < builders.size(); i++) {
                ProcessBuilder processBuilder = builders.get(i);
                if (i > 0) {
                    ProcessBuilder.Redirect redirect = processBuilder.redirectInput();
                    if (redirect != ProcessBuilder.Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectInput() must be PIPE except for the first builder: " + processBuilder.redirectInput());
                    }
                }
                if (i < builders.size() - 1) {
                    ProcessBuilder.Redirect redirect = processBuilder.redirectOutput();
                    if (redirect != ProcessBuilder.Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectOutput() must be PIPE except for the last builder: " + processBuilder.redirectOutput());
                    }
                }
                Process p = processBuilder.start();
                OutputStream nextInput = p.getOutputStream();
                if (i > 0) {
                    InputStream finalPrevOutput = prevOutput;
                    CompletableFuture.runAsync(() -> {
                        try {
                            J_I_InputStream.transferTo(finalPrevOutput, nextInput);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                processes.add(p);
                prevOutput = p.getInputStream();
            }
        } catch (Exception e) {
            for (Process process : processes) {
                process.destroyForcibly();
            }
            for (Process process : processes) {
                try {
                    process.waitFor();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            throw e;
        }
        return processes;
    }

}
