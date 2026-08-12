-- ===== 基础 CRUD 示例表 =====
create table if not exists sample_user (
    id varchar(64) not null,
    name varchar(128),
    email varchar(128),
    status int,
    primary key (id)
);

-- ===== 高级演示表 =====
create table if not exists sample_adv_category (
    id varchar(64) not null,
    name varchar(128),
    primary key (id)
);

create table if not exists sample_adv_product (
    id varchar(64) not null,
    name varchar(128),
    price decimal(12, 2),
    stock int,
    category_id varchar(64),
    status int,
    create_at timestamp,
    creator varchar(64),
    tenant_code varchar(64),
    update_at timestamp,
    updater varchar(64),
    primary key (id)
);

create table if not exists sample_adv_product_image (
    id varchar(64) not null,
    product_id varchar(64),
    url varchar(256),
    primary key (id)
);

-- 演示用 H2 表值函数 proc_product_stats(minStock) 不在此声明：
-- 其 CREATE ALIAS 的 Java 源码内含分号/双引号，放进本脚本会被 Spring 脚本切分器误切；
-- 改由 SampleH2FunctionInitializer 在启动期用 JdbcTemplate.execute 整体下发。
