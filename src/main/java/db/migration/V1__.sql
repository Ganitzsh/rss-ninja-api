CREATE TABLE "public"."users" (
  "id" serial,
  "email" varchar(255) NOT NULL,
  "password" varchar(60) NOT NULL,
  "first_name" varchar(60) NOT NULL,
  "last_name" varchar(60) NOT NULL,
  "username" varchar(60) NOT NULL,
  PRIMARY KEY ("id"),
  UNIQUE ("email")
);

CREATE TABLE "public"."feeds" (
  "id" serial,
  "url" varchar(512) NOT NULL,
  "name" varchar(60) NOT NULL,
  "owner_id" int NOT NULL REFERENCES "users",
  PRIMARY KEY ("id")
);

CREATE TABLE "public"."starred_item" (
  "id" serial,
  "type" varchar(512),
  "title" varchar(512),
  "author" varchar(512),
  "date_published" date,
  "date_updated" date,
  "description" varchar(512),
  "link" varchar(512),
  "owner_id" int NOT NULL REFERENCES "users",
  PRIMARY KEY ("id")
);

CREATE TABLE "public"."starred_item_json" (
  "id" serial,
  "content" varchar(1000000),
  "link" varchar(512),
  "owner_id" int NOT NULL REFERENCES "users",
  PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX item_json_pkey ON starred_item_json USING btree (link, owner_id);

CREATE TABLE "public"."categories" (
  "id" serial,
  "name" varchar(60),
  "color" varchar(6),
  "owner_id" int NOT NULL REFERENCES "users",
  PRIMARY KEY ("id")
);

CREATE TABLE "public"."category_feed" (
  "cat_id" int NOT NULL REFERENCES "categories",
  "feed_id" int NOT NULL REFERENCES "feeds"
);
CREATE UNIQUE INDEX category_feed_pkey ON category_feed USING btree (cat_id, feed_id);

create sequence hibernate_sequence;