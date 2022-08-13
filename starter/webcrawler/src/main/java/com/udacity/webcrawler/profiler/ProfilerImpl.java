package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
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
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    var interfaces = delegate.getClass().getInterfaces();
    var methods = interfaces[0].getMethods();
    var doesNotHaveAnyAnnotatedMethod = Arrays.stream(methods)
            .noneMatch(m -> m.isAnnotationPresent(Profiled.class));
    if (doesNotHaveAnyAnnotatedMethod) {
      throw new IllegalArgumentException();
    }
    var handler = new ProfilingMethodInterceptor(clock, state, delegate);
    return (T) Proxy.newProxyInstance(klass.getClassLoader(),
            new Class<?>[]{klass},
            handler);
  }

  @Override
  public void writeData(Path path) {
    var mode = Files.exists(path)
            ? StandardOpenOption.APPEND
            : StandardOpenOption.CREATE;
    try (var writer = Files.newBufferedWriter(path, mode)) {
      writeData(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
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
