package edu.udo.asynjobqueue

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private val queue = ConcurrentLinkedQueue<Runnable?>()
    private var future: Future<*>? = null

    override fun submit(job: Runnable) {
        if (future?.isDone != false) {
            submitForExecution(job)
        } else {
            queue.add(job)
        }
    }

    private fun submitForExecution(job: Runnable) {
        future = executor.submit {
            job.run()
            val next = queue.poll()
            if (next != null) {
                submitForExecution(next)
            }
        }
    }
}