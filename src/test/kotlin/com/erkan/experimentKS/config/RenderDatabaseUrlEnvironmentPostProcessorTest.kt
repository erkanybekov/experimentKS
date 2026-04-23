package com.erkan.experimentks.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.io.support.SpringFactoriesLoader

class RenderDatabaseUrlEnvironmentPostProcessorTest {

	@Test
	fun `converts Render postgres URL into JDBC properties`() {
		val result = RenderDatabaseUrlEnvironmentPostProcessor.DerivedDatasourceProperties.from(
			"postgresql://finance_user:se%24ret@db.internal:5432/experimentks?sslmode=require",
		)

		requireNotNull(result)
		assertEquals(
			"jdbc:postgresql://db.internal:5432/experimentks?sslmode=require",
			result.jdbcUrl,
		)
		assertEquals("finance_user", result.username)
		assertEquals("se\$ret", result.password)
	}

	@Test
	fun `ignores regular JDBC URLs`() {
		val result = RenderDatabaseUrlEnvironmentPostProcessor.DerivedDatasourceProperties.from(
			"jdbc:postgresql://localhost:5432/experimentks",
		)

		assertNull(result)
	}

	@Test
	fun `is registered through spring factories`() {
		val loadedPostProcessors = SpringFactoriesLoader.loadFactories(
			EnvironmentPostProcessor::class.java,
			javaClass.classLoader,
		)

		assertTrue(
			loadedPostProcessors.any { it is RenderDatabaseUrlEnvironmentPostProcessor },
			"RenderDatabaseUrlEnvironmentPostProcessor must be discoverable via META-INF/spring.factories",
		)
	}
}
