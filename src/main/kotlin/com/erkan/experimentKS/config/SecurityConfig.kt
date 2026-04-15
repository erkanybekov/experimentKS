package com.erkan.experimentKS.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.sessionManagement { session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			}
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(
						"/api/v1/experiments/**",
						"/api/v1/system/**",
						"/swagger-ui.html",
						"/swagger-ui/**",
						"/v3/api-docs/**",
					)
					.permitAll()
					.anyRequest()
					.authenticated()
			}
			.httpBasic(Customizer.withDefaults())

		return http.build()
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
