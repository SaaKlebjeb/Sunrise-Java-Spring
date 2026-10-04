--liquibase formatted sql

--changeset sunrise:002-seed-recipes
insert into recipes (title, cuisine, minutes, servings, internal_notes)
values ('Fish Amok', 'khmer', 45, 4, 'supplier: central market, pays late');

insert into ingredients (name, recipe_id)
values ('fish', (select id from recipes where title = 'Fish Amok'));