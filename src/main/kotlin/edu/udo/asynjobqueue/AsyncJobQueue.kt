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
    companion object {
        fun create(executor: ExecutorService): AsyncJobQueue = AsyncJobQueueImpl()
    }
}
