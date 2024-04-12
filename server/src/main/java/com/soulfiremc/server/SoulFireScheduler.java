/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server;

import com.soulfiremc.server.util.RandomUtil;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class SoulFireScheduler {
  private final ExecutorService managementExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private final PriorityQueue<TimedRunnable> executionQueue = new ObjectHeapPriorityQueue<>();
  private final ForkJoinPool mainThreadExecutor;
  private final Logger logger;
  private final Function<Runnable, Runnable> runnableWrapper;
  private boolean shutdown = false;

  public SoulFireScheduler(Logger logger) {
    this(logger, r -> r);
  }

  public SoulFireScheduler(Logger logger, Function<Runnable, Runnable> runnableWrapper) {
    this.mainThreadExecutor = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism(),
      ForkJoinPool.defaultForkJoinWorkerThreadFactory,
      null, true);
    this.logger = logger;
    this.runnableWrapper = runnableWrapper;

    this.managementExecutor.execute(this::managementTask);
  }

  @SuppressWarnings("BusyWait")
  public void managementTask() {
    try {
      while (!shutdown) {
        synchronized (executionQueue) {
          while (!executionQueue.isEmpty() && executionQueue.first().isReady()) {
            var timedRunnable = executionQueue.dequeue();
            schedule(() -> runCommand(timedRunnable.runnable()));
          }
        }

        Thread.sleep(1);
      }
    } catch (InterruptedException e) {
      logger.error("Error in executor", e);
    }
  }

  public void schedule(Runnable command) {
    if (shutdown) {
      return;
    }

    mainThreadExecutor.execute(() -> runCommand(command));
  }

  public void schedule(Runnable command, long delay, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    synchronized (executionQueue) {
      executionQueue.enqueue(TimedRunnable.of(command, delay, unit));
    }
  }

  public void scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    schedule(() -> {
      scheduleAtFixedRate(command, period, period, unit);
      runCommand(command);
    }, delay, unit);
  }

  public void scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    schedule(() -> {
      runCommand(command);
      scheduleWithFixedDelay(command, period, period, unit);
    }, delay, unit);
  }

  public void scheduleWithRandomDelay(Runnable command, long minDelay, long maxDelay, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    schedule(() -> {
      runCommand(command);
      scheduleWithRandomDelay(command, minDelay, maxDelay, unit);
    }, RandomUtil.getRandomLong(minDelay, maxDelay), unit);
  }

  public void shutdown() {
    shutdown = true;
    managementExecutor.shutdown();
    mainThreadExecutor.shutdown();
  }

  private void runCommand(Runnable command) {
    try {
      runnableWrapper.apply(command).run();
    } catch (Throwable t) {
      logger.error("Error in executor", t);
    }
  }

  private record TimedRunnable(Runnable runnable, long time) implements Comparable<TimedRunnable> {
    public static TimedRunnable of(Runnable runnable, long delay, TimeUnit unit) {
      return new TimedRunnable(runnable, System.currentTimeMillis() + unit.toMillis(delay));
    }

    @Override
    public int compareTo(TimedRunnable o) {
      return Long.compare(time, o.time);
    }

    public boolean isReady() {
      return System.currentTimeMillis() >= time;
    }
  }
}
