package edu.udo.asynjobqueue

internal data class Job(val runnable: Runnable, val future: FutureWrapper)