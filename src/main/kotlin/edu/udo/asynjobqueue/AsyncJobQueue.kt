package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService

/**
 * The {@link AsyncJobQueue} implements a FIFO queue of jobs which are run on
 * an {@link ExecutorService} provided during initialization. It is ensured
 * that at most one job runs at a time thus enforcing a sequential nature on
 * the queued jobs.
 *
 * In case no job is queued no resources are consumed because no polling or
 * blocking get operations are used for implementation.
 */
interface AsyncJobQueue {
    /**
     * Submits a job for execution. The job will be run on the thread pool
     * provided through the {@link ExecutorService} that was passed at
     * {@link AsyncJobQueue} creation time. The job will be scheduled for
     * execution only once all jobs that were scheduled prior were executed.
     * In case no job is currently waiting to be scheduled the given job will
     * be scheduled immediately.
     *
     * @param job The job to schedule.
     */
    fun submit(job: Runnable)

    companion object {
        /**
         * Creates a new {@link AsyncJobQueue}.
         *
         * @param executor The {@link ExecutorService} to schedule jobs on.
         */
        fun create(executor: ExecutorService): AsyncJobQueue = AsyncJobQueueImpl(executor)
    }
}
