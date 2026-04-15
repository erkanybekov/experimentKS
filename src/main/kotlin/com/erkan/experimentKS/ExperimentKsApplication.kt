package com.erkan.experimentKS

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ExperimentKsApplication

fun main(args: Array<String>) {
	runApplication<ExperimentKsApplication>(*args)
}
