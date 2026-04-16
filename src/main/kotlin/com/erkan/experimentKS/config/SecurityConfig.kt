package com.erkan.experimentks.config

import com.erkan.experimentks.shared.security.AuthenticatedUser
import com.erkan.experimentks.shared.security.JwtAccessTokenAuthenticationService
import com.erkan.experimentks.shared.security.RestAccessDeniedHandler
import com.erkan.experimentks.shared.security.RestAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.jwk.source.ImmutableSecret

@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	fun securityFilterChain(
		http: HttpSecurity,
		jwtAuthenticationConverter: Converter<Jwt, out AbstractAuthenticationToken>,
		restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
		restAccessDeniedHandler: RestAccessDeniedHandler,
	): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.sessionManagement { session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			}
			.exceptionHandling { exceptions ->
				exceptions
					.authenticationEntryPoint(restAuthenticationEntryPoint)
					.accessDeniedHandler(restAccessDeniedHandler)
			}
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(
						"/api/v1/auth/**",
						"/actuator/health",
						"/swagger-ui.html",
						"/swagger-ui/**",
						"/v3/api-docs/**",
						"/error",
					)
					.permitAll()
					.anyRequest()
					.authenticated()
			}
			.oauth2ResourceServer { oauth ->
				oauth.jwt { jwt ->
					jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
				}
			}
			.httpBasic(Customizer.withDefaults())

		return http.build()
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

	@Bean
	fun jwtAuthenticationConverter(
		jwtAccessTokenAuthenticationService: JwtAccessTokenAuthenticationService,
	): Converter<Jwt, out AbstractAuthenticationToken> = Converter { jwt ->
		val principal: AuthenticatedUser = jwtAccessTokenAuthenticationService.fromDecodedJwt(jwt)
		UsernamePasswordAuthenticationToken.authenticated(principal, jwt.tokenValue, emptyList())
	}

	@Bean
	fun jwtSecretKey(jwtProperties: JwtProperties): SecretKey =
		SecretKeySpec(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

	@Bean
	fun jwtEncoder(secretKey: SecretKey): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey))

	@Bean
	fun jwtDecoder(
		secretKey: SecretKey,
		jwtProperties: JwtProperties,
	): JwtDecoder {
		val validator: OAuth2TokenValidator<Jwt> =
			DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer))

		return NimbusJwtDecoder.withSecretKey(secretKey)
			.build()
			.apply { setJwtValidator(validator) }
	}
}
