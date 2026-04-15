package com.erkan.experimentks.config

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class CoroutineConfig {

	@Bean(name = ["blockingTaskDispatcher"], destroyMethod = "close")
	fun blockingTaskDispatcher(): ExecutorCoroutineDispatcher =
		Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
		) { runnable ->
			Thread(runnable, "blocking-task-dispatcher").apply { isDaemon = true }
		}.asCoroutineDispatcher()
}
