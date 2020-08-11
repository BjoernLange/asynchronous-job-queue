package edu.udo.asyncjobqueue

import edu.udo.asynjobqueue.AsyncJobQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `When three jobs are submitted then they are executed in order`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val job1 = mock(Runnable::class.java)
        jobQueue.submit(job1)
        val job2 = mock(Runnable::class.java)
        jobQueue.submit(job2)
        val job3 = mock(Runnable::class.java)
        jobQueue.submit(job3)

        // when:
        executor.runNext()
        executor.runNext()
        executor.runNext()

        // then:
        assertThat(executor.submitted).isEmpty()
        verify(job1).run()
        verify(job2).run()
        verify(job3).run()
    }

    @Test
    fun `When a job throws an exception then the next scheduled job is still executed`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val crashingJob = Runnable { throw IllegalArgumentException() }
        val job = mock(Runnable::class.java)

        jobQueue.submit(crashingJob)
        jobQueue.submit(job)

        // when:
        executor.runNext()
        executor.runNext()

        // then:
        verify(job).run()
    }

    @Test
    fun `When a job is submitted then it can be checked whether it is done`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val future = jobQueue.submit(Runnable { })
        assertFalse(future.isDone)

        // when:
        executor.runNext()

        // then:
        assertTrue(future.isDone)
    }

    @Test
    fun `When two jobs are submitted then it can be checked whether the second one is done`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { })
        val future = jobQueue.submit(Runnable { })
        assertFalse(future.isDone)

        // when:
        executor.runNext()
        executor.runNext()

        // then:
        assertTrue(future.isDone)
    }

    @Test
    fun `When the scheduling thread waits for the job to complete then the job does complete prior to the thread waking up`() {
        // given:
        val executor = Executors.newFixedThreadPool(1);
        val jobQueue = AsyncJobQueue.create(executor)

        var completed = false
        val future = jobQueue.submit(Runnable {
            Thread.sleep(200)
            completed = true
        })

        // when:
        future.get()

        // then:
        assertTrue(completed)
    }

    @Test
    fun `When the scheduling thread waits for the second job to complete then the job does complete prior to the thread waking up`() {
        // given:
        val executor = Executors.newFixedThreadPool(1);
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { Thread.sleep(200) })

        var completed = false
        val future = jobQueue.submit(Runnable {
            Thread.sleep(200)
            completed = true
        })

        // when:
        future.get()

        // then:
        assertTrue(completed)
    }

    @Test
    fun `When the scheduling thread waits for the job to complete with timeout then the job does complete prior to the thread waking up`() {
        // given:
        val executor = Executors.newFixedThreadPool(1);
        val jobQueue = AsyncJobQueue.create(executor)

        var completed = false
        val future = jobQueue.submit(Runnable {
            Thread.sleep(200)
            completed = true
        })

        // when:
        future.get(1, TimeUnit.SECONDS)

        // then:
        assertTrue(completed)
    }
}