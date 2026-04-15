package com.erkan.experimentks.finance.transaction.domain

import com.erkan.experimentks.finance.TransactionType
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

object TransactionSpecifications {

	fun activeForUser(userId: UUID): Specification<Transaction> = Specification { root, _, builder ->
		builder.and(
			builder.equal(root.get<Any>("user").get<UUID>("id"), userId),
			builder.isNull(root.get<Instant>("deletedAt")),
		)
	}

	fun occurredFrom(from: Instant): Specification<Transaction> = Specification { root, _, builder ->
		builder.greaterThanOrEqualTo(root.get("occurredAt"), from)
	}

	fun occurredTo(to: Instant): Specification<Transaction> = Specification { root, _, builder ->
		builder.lessThanOrEqualTo(root.get("occurredAt"), to)
	}

	fun withCategory(categoryId: UUID): Specification<Transaction> = Specification { root, _, builder ->
		builder.equal(root.get<Any>("category").get<UUID>("id"), categoryId)
	}

	fun withType(type: TransactionType): Specification<Transaction> = Specification { root, _, builder ->
		builder.equal(root.get<TransactionType>("type"), type)
	}
}
