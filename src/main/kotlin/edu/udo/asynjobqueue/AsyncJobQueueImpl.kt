package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private var future: Future<*>? = null
    private var next: Runnable? = null
    private var furtherNext: Runnable? = null

    override fun submit(job: Runnable) {
        if (future?.isDone != false) {
            future = executor.submit(Runnable {
                job.run()
                val nr = next
                if (nr != null) {
                    executor.submit(Runnable {
                        nr.run()
                        val nrr = furtherNext
                        if (nrr != null) {
                            executor.submit(nrr)
                        }
                    })
                }
            })
        } else if (next == null) {
            next = job
        } else {
            furtherNext = job
        }
    }
}