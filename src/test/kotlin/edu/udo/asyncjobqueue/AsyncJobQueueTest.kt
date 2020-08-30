package edu.udo.asyncjobqueue

import edu.udo.asynjobqueue.AsyncJobQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
        val executor = Executors.newFixedThreadPool(1)
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
        val executor = Executors.newFixedThreadPool(1)
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
        val executor = Executors.newFixedThreadPool(1)
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

    @Test
    fun `When the job is not scheduled until it times out then a TimeoutException is thrown`() {
        // given:
        val executor = Executors.newFixedThreadPool(1)
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { Thread.sleep(1000) })

        val future = jobQueue.submit(Runnable {})

        // when:
        Assertions.assertThrows(TimeoutException::class.java) {
            future.get(1, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun `When a job times out during execution then a TimeoutException is thrown`() {
        // given:
        val executor = Executors.newFixedThreadPool(1)
        val jobQueue = AsyncJobQueue.create(executor)

        val future = jobQueue.submit(Runnable {
            Thread.sleep(200)
        })

        // when:
        Assertions.assertThrows(TimeoutException::class.java) {
            future.get(1, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun `When the scheduling thread waits for a job and the sum of the wait stages exceeds the timeout then a TimeoutException is thrown`() {
        // given:
        val executor = Executors.newFixedThreadPool(1)
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { Thread.sleep(150) })
        val future = jobQueue.submit(Runnable {
            Thread.sleep(150)
        })

        // when:
        Assertions.assertThrows(TimeoutException::class.java) {
            future.get(200, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun `When a scheduled job is canceled before it was submitted for execution then the job is never run`() {
        // given:
        val executor = Executors.newFixedThreadPool(1)
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { Thread.sleep(200) })
        var jobWasInvoked = false
        val future = jobQueue.submit(Runnable { jobWasInvoked = true })

        // when:
        val result = future.cancel(true)

        // then:
        assertTrue(result)
        Thread.sleep(300)
        assertFalse(jobWasInvoked)
    }

    @Test
    fun `When a scheduled job is canceled after it was submitted for execution then it is interrupted`() {
        // given:
        val executor = Executors.newFixedThreadPool(1)
        val jobQueue = AsyncJobQueue.create(executor)

        var jobWasInterrupted = false
        val future = jobQueue.submit(Runnable {
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                jobWasInterrupted = true
            }
        })
        Thread.sleep(100)

        // when:
        val result = future.cancel(true)

        // then:
        assertTrue(result)
        assertTrue(jobWasInterrupted)
    }

    @Test
    fun `When a scheduled job is running then it is not cancelled`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val future = jobQueue.submit(Runnable { })

        // when:
        val result = future.isCancelled

        // then:
        assertFalse(result)
    }

    @Test
    fun `When a scheduled job is cancelled before it is scheduled then it is cancelled`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        jobQueue.submit(Runnable { })
        val future = jobQueue.submit(Runnable { })
        future.cancel(true)

        // when:
        val result = future.isCancelled

        // then:
        assertTrue(result)
    }

    @Test
    fun `When a scheduled job is cancelled after it is scheduled then it is cancelled`() {
        // given:
        val executor = ManualExecutorService()
        val jobQueue = AsyncJobQueue.create(executor)

        val future = jobQueue.submit(Runnable { })
        future.cancel(true)

        // when:
        val result = future.isCancelled

        // then:
        assertTrue(result)
    }
}