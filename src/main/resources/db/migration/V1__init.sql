create table if not exists pictures
(
    id         bigserial primary key,
    url        varchar(255),
    width      int,
    height     int,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- insert into pictures (url, width, height)
-- values ('pictures\test.bmp', 200, 100);







