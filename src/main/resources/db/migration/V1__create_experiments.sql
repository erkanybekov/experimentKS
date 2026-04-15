create table experiments (
    id uuid primary key,
    title varchar(120) not null,
    description varchar(500) not null,
    platform varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_experiments_updated_at on experiments (updated_at);
