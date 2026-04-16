create table chat_rooms (
    id uuid primary key,
    created_by_user_id uuid not null references users(id),
    name varchar(120) not null,
    last_activity_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_chat_rooms_last_activity
    on chat_rooms (last_activity_at desc nulls last, created_at desc);

create table chat_room_members (
    id uuid primary key,
    room_id uuid not null references chat_rooms(id),
    user_id uuid not null references users(id),
    joined_at timestamp with time zone not null,
    last_read_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index uq_chat_room_members_room_user
    on chat_room_members (room_id, user_id);

create index idx_chat_room_members_user
    on chat_room_members (user_id);

create table chat_messages (
    id uuid primary key,
    room_id uuid not null references chat_rooms(id),
    sender_user_id uuid not null references users(id),
    client_message_id uuid not null,
    content varchar(2000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index uq_chat_messages_room_sender_client_message
    on chat_messages (room_id, sender_user_id, client_message_id);

create index idx_chat_messages_room_created
    on chat_messages (room_id, created_at desc);
