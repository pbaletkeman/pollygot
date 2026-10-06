package ca.letkeman

import java.time.LocalDateTime

data class Task(val taskId: String, val payload: String, val createTimestamp: LocalDateTime)
