package org.jetlinks.core.message.codec.parser;

import lombok.SneakyThrows;
import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * 通过 JUnit 触发 JMH 解析器基准（短迭代，仅验证可运行）.
 * <p>
 * 运行: {@code mvn test -Dtest=ParserJmhBenchmarkTest}
 * </p>
 */
public class ParserJmhBenchmarkTest {

    @SneakyThrows
    public static void main(String[] args) {
        Options opt = new OptionsBuilder()
            .include(ParserJmhBenchmark.class.getSimpleName())
            .warmupIterations(1)
            .measurementIterations(1)
            .forks(0)
            .build();
        new Runner(opt).run();
    }
}
