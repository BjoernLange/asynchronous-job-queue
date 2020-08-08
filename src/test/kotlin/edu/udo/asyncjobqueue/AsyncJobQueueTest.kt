package edu.udo.asyncjobqueue

import edu.udo.asynjobqueue.AsyncJobQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

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
    fun `When a job is submitted and no job is running then it is immediately submitted for execution`() {
        // given:
        val executor = mock(ExecutorService::class.java)
        val jobQueue = AsyncJobQueue.create(executor)
        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        verify(executor).submit(job)
    }

    @Test
    fun `When a job is submitted while another is running then the job is not submitted for execution`() {
        // given:
        val executor = mock(ExecutorService::class.java)
        `when`(executor.submit(ArgumentMatchers.any())).thenReturn(CompletableFuture<Void>())
        val jobQueue = AsyncJobQueue.create(executor)

        val runningJob = Runnable {}
        jobQueue.submit(runningJob)

        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        verify(executor).submit(runningJob)
        verifyNoMoreInteractions(executor)
    }

    @Test
    fun `When a job is submitted after another completed then the job is submitted for execution`() {
        // given:
        val future = CompletableFuture<Void?>()

        val executor = mock(ExecutorService::class.java)
        `when`(executor.submit(ArgumentMatchers.any())).thenReturn(future)
        val jobQueue = AsyncJobQueue.create(executor)

        val completedJob = Runnable {}
        jobQueue.submit(completedJob)

        future.complete(null)

        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        verify(executor).submit(completedJob)
        verify(executor).submit(job)
    }

    @Test
    fun `When a running job completes then the next job is submitted for execution`() {
        // given:
        var scheduledJob: Runnable? = null
        val future = CompletableFuture<Void?>()

        val executor = mock(ExecutorService::class.java)
        `when`(executor.submit(ArgumentMatchers.any())).thenAnswer(Answer<Future<*>> {
            if (scheduledJob == null) {
                val arg0 = it.arguments[0]
                if (arg0 is Runnable) {
                    scheduledJob = arg0
                }
                return@Answer future
            } else {
                return@Answer CompletableFuture<Void?>()
            }
        })
        val jobQueue = AsyncJobQueue.create(executor)

        val completingJob = Runnable {}
        jobQueue.submit(completingJob)
        val job = Runnable {}
        jobQueue.submit(job)

        verify(executor).submit(completingJob)
        verifyNoMoreInteractions(executor)
        assertThat(scheduledJob).isNotNull

        // when:
        scheduledJob.run { }

        // then:
        verify(executor).submit(job)
        verifyNoMoreInteractions(executor)
    }
}