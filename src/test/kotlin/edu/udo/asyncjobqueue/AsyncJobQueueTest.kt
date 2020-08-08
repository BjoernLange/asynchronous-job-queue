package edu.udo.asyncjobqueue

import edu.udo.asynjobqueue.AsyncJobQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
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
    fun `When a job is submitted and no job is running then it is immediately submitted for execution`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)
        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        assertThat(executor.submitted).hasSize(1)
    }

    @Test
    fun `When a job is submitted then it is executed`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)
        val job = mock(Runnable::class.java)
        jobQueue.submit(job)

        // when:
        executor.runNext()

        // then:
        assertThat(executor.submitted).isEmpty()
        verify(job).run()
    }

    @Test
    fun `When a job is submitted while another is running then the job is not submitted for execution`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val runningJob = Runnable {}
        jobQueue.submit(runningJob)

        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        assertThat(executor.submitted).hasSize(1)
    }

    @Test
    fun `When a job is submitted after another completed then the job is submitted for execution`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val completedJob = Runnable {}
        jobQueue.submit(completedJob)
        executor.runNext()

        val job = Runnable {}

        // when:
        jobQueue.submit(job)

        // then:
        assertThat(executor.submitted).hasSize(1)
    }

    @Test
    fun `When a running job completes then the next job is submitted for execution`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val completingJob = mock(Runnable::class.java)
        jobQueue.submit(completingJob)
        val job = Runnable {}
        jobQueue.submit(job)

        assertThat(executor.submitted).hasSize(1)

        // when:
        executor.runNext()

        // then:
        assertThat(executor.submitted).hasSize(1)
        verify(completingJob).run()
    }
}