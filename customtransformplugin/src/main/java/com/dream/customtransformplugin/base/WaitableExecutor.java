package com.dream.customtransformplugin.base;

import static com.google.common.base.Preconditions.checkArgument;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.Immutable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * function: 线程池：允许等待所有提交任务执行完
 *
 * @author zy
 * @since 2022/10/29
 */
public class WaitableExecutor {

    @NonNull
    private final ForkJoinPool forkJoinPool;
    private final boolean owned;
    @NonNull private final Set<ForkJoinTask<?>> futureSet = Sets.newConcurrentHashSet();

    WaitableExecutor(@NonNull ForkJoinPool forkJoinPool, boolean owned) {
        this.forkJoinPool = forkJoinPool;
        this.owned = owned;
    }

    /**
     * Creates a new {@link WaitableExecutor} which uses a globally shared thread pool.
     *
     * <p>Calling {@link #waitForAllTasks()} on this instance will only block on tasks submitted to
     * this instance, but the tasks themselves will compete for threads with tasks submitted to
     * other {@link WaitableExecutor} instances created with this factory method.
     *
     * <p>This is the recommended way of getting a {@link WaitableExecutor}, since it makes sure the
     * total number of threads running doesn't exceed the value configured by the user.
     *
     */
    public static WaitableExecutor useGlobalSharedThreadPool() {
        return new WaitableExecutor(ForkJoinPool.commonPool(), false);
    }

    /**
     * Creates a new {@link WaitableExecutor} which uses a newly allocated thread pool of the given
     * size.
     *
     * <p>If you can, use the {@link #useGlobalSharedThreadPool()} factory method instead.
     *
     * @see #useGlobalSharedThreadPool()
     */
    public static WaitableExecutor useNewFixedSizeThreadPool(int nThreads) {
        checkArgument(nThreads > 0, "Number of threads needs to be a positive number.");
        return new WaitableExecutor(new ForkJoinPool(nThreads), true);

    }

    /**
     * Creates a new {@link WaitableExecutor} that executes all jobs on the thread that schedules
     * them, removing any concurrency.
     *
     * @see MoreExecutors#newDirectExecutorService()
     */
    @VisibleForTesting
    @SuppressWarnings("unused") // Temporarily used when debugging.
    public static WaitableExecutor useDirectExecutor() {
        return new WaitableExecutor(new ForkJoinPool(1), true);
    }

    /**
     * Submits a Callable for execution.
     *
     * @param callable the callable to run.
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     */
    public synchronized <T> ForkJoinTask<T> execute(Callable<T> callable) {
        ForkJoinTask<T> submitted = forkJoinPool.submit(callable);
        boolean added = futureSet.add(submitted);
        Preconditions.checkState(added, "Failed to add task");
        return submitted;
    }

    /**
     * Returns the number of tasks that have been submitted for execution but the results have not
     * been fetched yet.
     */
    int getUnprocessedTasksCount() {
        return futureSet.size();
    }

    /**
     * Waits for all tasks to be executed. If a tasks throws an exception, it will be thrown from
     * this method inside a RuntimeException, preventing access to the result of the other threads.
     *
     * <p>If you want to get the results of all tasks (result and/or exception), use {@link
     * #waitForAllTasks()}
     *
     * <p>To get the actual cause of the failure, examine the exception thrown. There are some
     * nuances to it though. If the exception was thrown on the same thread on which we wait for
     * completion, the {@link Throwable#getCause()} will be {@code null}. If the exception was
     * thrown on a different thread, the fork join pool mechanism will try to set the cause. Because
     * there is no access to this information, you probably want to check for the cause first, and
     * only if it is null, to check the exception thrown by this method.
     *
     * @param cancelRemaining if true, and a task fails, cancel all remaining tasks.
     * @return a list of all the return values from the tasks.
     * @throws InterruptedException if this thread was interrupted. Not if the tasks were
     *     interrupted.
     */
    public synchronized <T> List<T> waitForTasksWithQuickFail(boolean cancelRemaining)
            throws InterruptedException {
        List<T> results = Lists.newArrayListWithCapacity(futureSet.size());

        try {
            for (ForkJoinTask<?> future : futureSet) {
                results.add((T) future.join());
            }
        } catch (RuntimeException | Error e) {
            if (cancelRemaining) {
                cancelAllTasks();
            }
            throw e;
        } finally {
            futureSet.clear();
            if (owned) {
                forkJoinPool.shutdownNow();
            }
        }

        return results;
    }

    @Immutable
    public static final class TaskResult<T> {
        @Nullable
        private final T value;
        @Nullable private final Throwable exception;

        TaskResult(@Nullable T value) {
            this.value = value;
            this.exception = null;
        }

        TaskResult(@NonNull Throwable exception) {
            this.value = null;
            this.exception = Preconditions.checkNotNull(exception);
        }

        @Nullable
        public T getValue() {
            return value;
        }

        @Nullable
        public Throwable getException() {
            return exception;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("value", getValue())
                    .add("exception", getException())
                    .toString();
        }
    }

    /**
     * Waits for all tasks to be executed, and returns a {@link TaskResult} for each, containing
     * either the result or the exception thrown by the task.
     *
     * <p>If a task is cancelled (and it threw InterruptedException) then the result for the task is
     * *not* included.
     *
     * @return a list of all the return values from the tasks.
     * @throws InterruptedException if this thread was interrupted. Not if the tasks were
     *     interrupted.
     */
    @NonNull
    public synchronized <T> List<TaskResult<T>> waitForAllTasks() throws InterruptedException {
        List<TaskResult<T>> results = Lists.newArrayListWithCapacity(futureSet.size());
        try {
            for (ForkJoinTask<?> future : futureSet) {
                // Get the result from the task.
                try {
                    results.add(new TaskResult((T) future.join()));
                } catch (RuntimeException e) {
                    // the original exception thrown by the task is the cause of this one.
                    results.add(new TaskResult<>(e.getCause() != null ? e.getCause() : e));
                } catch (Error e) {
                    results.add(new TaskResult<>(e));
                }
            }
        } finally {
            futureSet.clear();
            if (owned) {
                forkJoinPool.shutdownNow();
            }
        }

        return results;
    }

    /** Cancel all remaining tasks. */
    public synchronized void cancelAllTasks() {
        for (Future<?> future : futureSet) {
            future.cancel(true /*mayInterruptIfRunning*/);
        }
        futureSet.clear();
    }

    /** Returns the parallelism of this executor i.e. how many tasks can run in parallel. */
    public int getParallelism() {
        return forkJoinPool.getParallelism();
    }
}
