create table if not exists l1_to_l2_txns
(
    version             integer not null,
    index               bigint  not null,
    l1_block_number     bigint,
    l1_block_timestamp  timestamp,
    l1_block_hash       varchar(66),
    l1_transaction_hash varchar(66),
    l1_from_address     varchar(42),
    l1_to_address       varchar(42),
    l2_block_number     bigint,
    l2_block_timestamp  timestamp,
    l2_block_hash       varchar(66),
    l2_transaction_hash varchar(66),
    l2_from_address     varchar(42),
    l2_to_address       varchar(42),
    amount              numeric(78),
    from_address        varchar(42),
    to_address          varchar(42),
    l1_token_address    varchar(42),
    l2_token_address    varchar(42),
    extra_info          jsonb,
    _type               integer,
    deposit_hash        varchar,
    primary key (version, index)
);

create index if not exists idx_l1_to_l2_index_type
    on l1_to_l2_txns (_type asc, index desc);

create index if not exists idx_l1_to_l2_from_address_l1_block_number
    on l1_to_l2_txns (from_address asc, l1_block_number desc);

create index if not exists idx_l1_to_l2_to_address_l1_block_number
    on l1_to_l2_txns (to_address asc, l1_block_number desc);

create table l2_to_l1_txns
(
    version                      integer not null,
    index                        bigint  not null,
    l1_block_number              bigint,
    l1_block_timestamp           timestamp,
    l1_block_hash                varchar(66),
    l1_transaction_hash          varchar(66),
    l1_from_address              varchar(42),
    l1_to_address                varchar(42),
    l2_block_number              bigint,
    l2_block_timestamp           timestamp,
    l2_block_hash                varchar(66),
    l2_transaction_hash          varchar(66),
    l2_from_address              varchar(42),
    l2_to_address                varchar(42),
    amount                       numeric(78),
    from_address                 varchar(42),
    to_address                   varchar(42),
    l1_token_address             varchar(42),
    l2_token_address             varchar(42),
    extra_info                   jsonb,
    _type                        integer,
    withdrawal_hash              varchar,
    l1_proven_transaction_hash           varchar(66),
    l1_proven_block_number       bigint,
    l1_proven_block_timestamp    timestamp,
    l1_proven_from_address       varchar(42),
    l1_proven_to_address         varchar(42),
    l1_finalized_transaction_hash        varchar(66),
    l1_finalized_block_number    bigint,
    l1_finalized_block_timestamp timestamp,
    l1_finalized_from_address    varchar(42),
    l1_finalized_to_address      varchar(42),
    primary key (version, index)
);

create index if not exists idx_l2_to_l1_from_address_l2_block_number
    on l2_to_l1_txns (from_address asc, l2_block_number desc);
create index if not exists idx_l2_to_l1_to_address_l2_block_number
    on l2_to_l1_txns (to_address asc, l2_block_number desc);

create table if not exists op_bedrock_state_batches
(
    batch_index         bigint not null
        primary key,
    l1_block_number     bigint,
    l1_block_timestamp  timestamp,
    l1_block_hash       varchar(66),
    l1_transaction_hash varchar(66),
    batch_root          varchar,
    start_block_number  bigint,
    end_block_number    bigint,
    block_count         integer generated always as (((end_block_number - start_block_number) + 1)) stored
);

create index if not exists idx_l1_block_timestamp_op_bedrock_state_batches_desc
    on op_bedrock_state_batches (l1_block_timestamp desc);

create index if not exists idx_l2_block_op_bedrock_state_batches_numbers
    on op_bedrock_state_batches (start_block_number, end_block_number);

