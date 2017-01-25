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

create sequence hibernate_sequence;