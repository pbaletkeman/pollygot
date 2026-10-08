package ca.letkeman.week1.model.task

import java.time.LocalDateTime

val SHUTDOWN = -1;
data class Task(val taskId: String, val payload: String, val createTimestamp: LocalDateTime)
