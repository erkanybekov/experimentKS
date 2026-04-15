package com.erkan.experimentKS.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {

	@Bean
	fun utcClock(): Clock = Clock.systemUTC()
}
