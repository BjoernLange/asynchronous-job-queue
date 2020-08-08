package edu.udo.asyncjobqueue

import edu.udo.asynjobqueue.AsyncJobQueue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import java.util.concurrent.ExecutorService

class AsyncJobQueueTest {
    @Test
    fun `When a job queue is created then no jobs are run`() {
        // given:
        val executor = mock(ExecutorService::class.java)

        // when:
        AsyncJobQueue.create(executor)

        // then:
        verifyNoInteractions(executor)
    }

    @Test
    fun `When a job is queued and no job is running then it is immediately submitted for execution`() {
        // given:
        val executor = mock(ExecutorService::class.java)
        val jobQueue = AsyncJobQueue.create(executor)
        val job = Runnable {}

        // when:
        jobQueue.submit(job)
    }
}