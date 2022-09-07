package org.jetlinks.core.benchmark;

import lombok.SneakyThrows;
import org.jetlinks.core.utils.SerializeUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SerializeUtilsTest {


    @Benchmark
    @SneakyThrows
    public void testObject(Blackhole blackhole) {
        String str = "1234567890";
        try (ObjectOutput objectOutput = new ObjectOutputStream(new ByteArrayOutputStream())) {
            SerializeUtils.writeObject(str, objectOutput);
        }
    }

    public static void main(String[] args) throws RunnerException {
        BenchmarkRunner.run(SerializeUtilsTest.class);
    }
}
