package com.api.ealsticsearch.config

import com.api.ealsticsearch.enums.AGGREGATIONS_TYPE
import com.api.ealsticsearch.service.ElasticSearchRankService
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import reactor.core.publisher.Mono


@Configuration
@EnableBatchProcessing
class BatchConfig(
    private val elasticSearchRankService: ElasticSearchRankService,
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager
) {
    @Bean
    fun jobLauncher(): JobLauncher {
        val taskExecutorLauncher = TaskExecutorJobLauncher()
        taskExecutorLauncher.setTaskExecutor(SimpleAsyncTaskExecutor())
        taskExecutorLauncher.setJobRepository(jobRepository)
        taskExecutorLauncher.afterPropertiesSet()
        return taskExecutorLauncher
    }

    @Bean
    fun rankingUpdateJob(rankingUpdateStep: Step): Job {
        return JobBuilder("rankingUpdateJob", jobRepository)
            .start(rankingUpdateStep)
            .build()
    }

    @Bean
    fun rankingUpdateStep(): Step {
        return StepBuilder("rankingUpdateStep", jobRepository)
            .tasklet({ _, _ ->
                runBatch()
                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
    }

    @Bean
    fun runJob(jobLauncher: JobLauncher, rankingUpdateJob: Job): CommandLineRunner {
        return CommandLineRunner {
            val jobParameters = JobParametersBuilder().toJobParameters()
            jobLauncher.run(rankingUpdateJob, jobParameters)
        }
    }


    private fun runBatch() {
        println("Running batch job...")
        val oneHour = elasticSearchRankService.updateRanking(AGGREGATIONS_TYPE.ONE_HOURS, 30)
            .flatMap { elasticSearchRankService.saveRankings(it) }

        val sixHours = elasticSearchRankService.updateRanking(AGGREGATIONS_TYPE.SIX_HOURS, 30)
            .flatMap { elasticSearchRankService.saveRankings(it) }

        val oneDay = elasticSearchRankService.updateRanking(AGGREGATIONS_TYPE.ONE_DAY, 30)
            .flatMap { elasticSearchRankService.saveRankings(it) }

        val sevenDays = elasticSearchRankService.updateRanking(AGGREGATIONS_TYPE.SEVEN_DAY, 30)
            .flatMap { elasticSearchRankService.saveRankings(it) }

        Mono.`when`(oneHour, sixHours, oneDay, sevenDays)
            .doOnSuccess { println("Batch job completed successfully.") }
            .doOnError { error -> println("Batch job failed with error: $error") }
            .block()
    }


}