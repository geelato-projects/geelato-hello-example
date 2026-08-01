create table if not exists sample_product (
    id varchar(64) not null,
    name varchar(128),
    price decimal(12, 2),
    stock int,
    primary key (id)
);
