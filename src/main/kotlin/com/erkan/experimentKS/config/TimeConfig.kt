package com.erkan.experimentks.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {

	@Bean
	fun utcClock(): Clock = Clock.systemUTC()
}
