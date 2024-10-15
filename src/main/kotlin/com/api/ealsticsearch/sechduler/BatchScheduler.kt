package com.api.ealsticsearch.sechduler

import org.springframework.batch.core.Job
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchScheduler(
    private val jobLauncher: JobLauncher,
    private val rankingUpdateJob: Job
) {

    @Scheduled(cron = "0 0 * * * *")
    fun runRankingUpdateJob() {
        println("Triggering batch job at ${java.time.LocalDateTime.now()}")

        try {
            val jobParameters = org.springframework.batch.core.JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            jobLauncher.run(rankingUpdateJob, jobParameters)
        } catch (e: Exception) {
            println("Batch job failed: ${e.message}")
        }
    }
}