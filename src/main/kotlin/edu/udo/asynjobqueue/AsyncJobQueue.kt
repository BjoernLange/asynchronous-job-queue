package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService

interface AsyncJobQueue {
    companion object {
        fun create(executor: ExecutorService): AsyncJobQueue = AsyncJobQueueImpl()
    }
}
