create table users (
    id uuid primary key,
    email varchar(255) not null,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create unique index uq_users_email_active on users (lower(email)) where deleted_at is null;

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id),
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_refresh_tokens_user_id on refresh_tokens (user_id);

create table categories (
    id uuid primary key,
    user_id uuid not null references users(id),
    type varchar(16) not null,
    name varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create unique index uq_categories_user_type_name_active
    on categories (user_id, type, lower(name))
    where deleted_at is null;

create index idx_categories_user_active on categories (user_id)
    where deleted_at is null;

create table transactions (
    id uuid primary key,
    user_id uuid not null references users(id),
    category_id uuid not null references categories(id),
    type varchar(16) not null,
    note varchar(500),
    amount numeric(19, 2) not null,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create index idx_transactions_user_occurred_active
    on transactions (user_id, occurred_at desc)
    where deleted_at is null;

create index idx_transactions_user_updated
    on transactions (user_id, updated_at desc);

create index idx_transactions_user_category_active
    on transactions (user_id, category_id)
    where deleted_at is null;
