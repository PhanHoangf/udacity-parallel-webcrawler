package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass);
        boolean hasProfiledMethod = Arrays.stream(klass.getMethods())
                .anyMatch(method -> method.getAnnotation(Profiled.class) != null);

        if (!hasProfiledMethod) {
            throw new IllegalArgumentException("The wrapped interface does not contain a @Profiled method.");
        }
        // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
        //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
        //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
        return (T) Proxy.newProxyInstance(
                klass.getClassLoader(),
                new Class[]{klass},
                new ProfilingMethodInterceptor(clock, delegate, state));
    }

    @Override
    public void writeData(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
            state.write(writer);
            writer.newLine();
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
