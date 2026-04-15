package com.erkan.experimentks.finance.category.domain

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.shared.domain.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class Category() : SoftDeletableEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	lateinit var user: User

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	var type: TransactionType = TransactionType.EXPENSE

	@Column(nullable = false, length = 80)
	lateinit var name: String

	constructor(
		user: User,
		type: TransactionType,
		name: String,
	) : this() {
		this.user = user
		this.type = type
		this.name = name
	}
}
