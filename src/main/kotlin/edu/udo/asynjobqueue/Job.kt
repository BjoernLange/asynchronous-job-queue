package edu.udo.asynjobqueue

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

internal data class Job(val runnable: Runnable, val future: CompletableFuture<Future<*>> = CompletableFuture())