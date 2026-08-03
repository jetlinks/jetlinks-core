package org.jetlinks.core.benchmark;

import org.jetlinks.core.utils.DistinctDurationFlux;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 大量活跃 distinct 的保留内存压力工具。
 *
 * 每次运行只测一种 distinct 数量和数据量，通过独立 JVM 隔离上一组对象与 GC 状态。
 * 采样前使用 jcmd 触发 GC，并保留 heap、RSS、NMT 和 class histogram 证据。
 * 第五个参数传入 {@code raw} 时只订阅原始 source，用于扣除订阅链和压力工具自身开销。
 */
public final class DistinctDurationFluxMemoryStress {

    private DistinctDurationFluxMemoryStress() {
    }

    public static void main(String[] args) throws Exception {
        int distinctCount = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        int dataSize = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        long ttlMillis = args.length > 2 ? Long.parseLong(args[2]) : 1_000;
        String resultName = args.length > 3 ? args[3] : "memory";
        boolean rawSubscription = args.length > 4 && "raw".equals(args[4]);

        Path resultDirectory = Paths.get("target", "distinct-duration-benchmark", resultName);
        Files.createDirectories(resultDirectory);

        initializeParallelScheduler();
        Sample baseline = sample("baseline", resultDirectory);

        List<Disposable> subscriptions = createSubscriptions(
            distinctCount,
            dataSize,
            ttlMillis,
            rawSubscription);
        Sample active = sample("active", resultDirectory);

        awaitTtl(Duration.ofMillis(ttlMillis));
        Sample postTtlIdle = sample("post-ttl-idle", resultDirectory);

        subscriptions.forEach(Disposable::dispose);
        subscriptions.clear();
        Sample postCancel = sample("post-cancel", resultDirectory);

        String summary = buildSummary(
            distinctCount,
            dataSize,
            ttlMillis,
            rawSubscription ? "raw" : "distinct",
            baseline,
            active,
            postTtlIdle,
            postCancel);
        Files.write(
            resultDirectory.resolve("summary.csv"),
            summary.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
        System.out.print(summary);
    }

    private static void initializeParallelScheduler() throws InterruptedException {
        CountDownLatch initialized = new CountDownLatch(1);
        Schedulers.parallel().schedule(initialized::countDown);
        if (!initialized.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("parallel scheduler initialization timed out");
        }
    }

    private static List<Disposable> createSubscriptions(int distinctCount,
                                                         int dataSize,
                                                         long ttlMillis,
                                                         boolean rawSubscription) {
        List<Disposable> subscriptions = new ArrayList<>(distinctCount);
        Duration ttl = Duration.ofMillis(ttlMillis);
        Flux<Integer> source = dataSize == 0
            ? Flux.never()
            : Flux.concat(Flux.range(0, dataSize), Flux.never());

        for (int i = 0; i < distinctCount; i++) {
            Flux<Integer> target = rawSubscription
                ? source
                : DistinctDurationFlux.create(source, Function.identity(), ttl);
            subscriptions.add(target.subscribe());
        }
        return subscriptions;
    }

    private static void awaitTtl(Duration ttl) throws InterruptedException {
        CountDownLatch elapsed = new CountDownLatch(1);
        long delay = Math.multiplyExact(ttl.toMillis(), 3);
        Schedulers.parallel().schedule(elapsed::countDown, delay, TimeUnit.MILLISECONDS);
        if (!elapsed.await(delay + 10_000, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("TTL wait timed out");
        }
    }

    private static Sample sample(String phase, Path resultDirectory) throws Exception {
        long pid = ProcessHandle.current().pid();
        runJcmd(pid, resultDirectory.resolve(phase + "-gc.txt"), "GC.run");

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long rssBytes = readRssBytes(pid);

        runJcmd(pid, resultDirectory.resolve(phase + "-histogram.txt"), "GC.class_histogram");
        runJcmd(pid, resultDirectory.resolve(phase + "-nmt.txt"), "VM.native_memory", "summary");
        return new Sample(phase, heapUsed, rssBytes);
    }

    private static void runJcmd(long pid, Path output, String... command) throws Exception {
        String jcmd = Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString();
        List<String> args = new ArrayList<>();
        args.add(jcmd);
        args.add(Long.toString(pid));
        for (String value : command) {
            args.add(value);
        }
        Process process = new ProcessBuilder(args)
            .redirectErrorStream(true)
            .redirectOutput(output.toFile())
            .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("jcmd failed with exit code " + exitCode + ": " + args);
        }
    }

    private static long readRssBytes(long pid) throws Exception {
        Process process = new ProcessBuilder("/bin/ps", "-o", "rss=", "-p", Long.toString(pid))
            .redirectErrorStream(true)
            .start();
        String value;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            value = reader.readLine();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0 || value == null) {
            throw new IllegalStateException("failed to read RSS for pid " + pid);
        }
        return Long.parseLong(value.trim()) * 1024L;
    }

    private static String buildSummary(int distinctCount,
                                       int dataSize,
                                       long ttlMillis,
                                       String operator,
                                       Sample... samples) {
        StringBuilder summary = new StringBuilder();
        summary.append(
            "operator,distinctCount,dataSize,ttlMillis,phase,heapUsed,rssBytes,heapDelta,bytesPerDistinct\n");
        long baselineHeap = samples[0].heapUsed;
        for (Sample sample : samples) {
            long heapDelta = sample.heapUsed - baselineHeap;
            long bytesPerDistinct = distinctCount == 0 ? 0 : heapDelta / distinctCount;
            summary
                .append(operator).append(',')
                .append(distinctCount).append(',')
                .append(dataSize).append(',')
                .append(ttlMillis).append(',')
                .append(sample.phase).append(',')
                .append(sample.heapUsed).append(',')
                .append(sample.rssBytes).append(',')
                .append(heapDelta).append(',')
                .append(bytesPerDistinct).append('\n');
        }
        return summary.toString();
    }

    private static final class Sample {
        private final String phase;
        private final long heapUsed;
        private final long rssBytes;

        private Sample(String phase, long heapUsed, long rssBytes) {
            this.phase = phase;
            this.heapUsed = heapUsed;
            this.rssBytes = rssBytes;
        }
    }
}
