package com.erkan.experimentks.finance.transaction.domain

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.category.domain.Category
import com.erkan.experimentks.shared.domain.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "transactions")
class Transaction() : SoftDeletableEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	lateinit var user: User

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	lateinit var category: Category

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	var type: TransactionType = TransactionType.EXPENSE

	@Column(length = 500)
	var note: String? = null

	@Column(nullable = false, precision = 19, scale = 2)
	var amount: BigDecimal = BigDecimal.ZERO

	@Column(name = "occurred_at", nullable = false)
	var occurredAt: Instant = Instant.EPOCH

	constructor(
		user: User,
		category: Category,
		type: TransactionType,
		note: String?,
		amount: BigDecimal,
		occurredAt: Instant,
	) : this() {
		this.user = user
		this.category = category
		this.type = type
		this.note = note
		this.amount = amount
		this.occurredAt = occurredAt
	}
}
