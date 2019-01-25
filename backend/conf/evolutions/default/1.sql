# Users schema

# --- !Ups

CREATE TABLE MEMBER(
  MEMBER_ID    INT         PRIMARY KEY NOT NULL,
  MEMBER_NAME  VARCHAR(255) NOT NULL,
  HASH         VARCHAR(255) NOT NULL,
  REALNAME     VARCHAR(255) NOT NULL,
  EMAIL        VARCHAR(255) NOT NULL,
  COUNTRY      VARCHAR(255) NOT NULL,
  COMPANY      VARCHAR(255),
  TITLE        VARCHAR(255),
  STUDENT_UNTIL DATE,
  PROFESSION   VARCHAR(255)
);

CREATE TABLE ACTIVITY(
  MEMER_ID     INT         REFERENCES MEMBER(MEMBER_ID) NOT NULL,
  TIME_FROM    DATE        NOT NULL,
  TIME_TO      DATE        NOT NULL,
  ACCOUNT_TYPE VARCHAR(255) NOT NULL,
  AMOUNT_PAID  VARCHAR(255)
);

CREATE TABLE INFO(
  ABBREV       VARCHAR(255) PRIMARY KEY NOT NULL,
  TITLE        VARCHAR(255) NOT NULL,
  LANGUAG      VARCHAR(255) NOT NULL,
  AUTHORLASTNAME  VARCHAR(255),
  AUTHORFIRSTNAME VARCHAR(255),
  YEARR        INT,
  PUBLISHER    VARCHAR(255),
  EDITION      INT,
  ACCESS       VARCHAR(255)
);

CREATE TABLE RUBRIC(
  ID          INT PRIMARY KEY NOT NULL,
  MOTHER      INT,
  ISMOTHER    BOOLEAN,
  CHAPTERID   INT NOT NULL,
  FULLPATH    VARCHAR(255) NOT NULL,
  PATH        VARCHAR(255),
  TEXTT       VARCHAR(255)
);

CREATE TABLE REMEDY(
  ID          INT PRIMARY KEY NOT NULL,
  NAMEABBREV  VARCHAR(10) NOT NULL,
  NAMELONG    VARCHAR(40) NOT NULL
);

-- https://stackoverflow.com/questions/14225397/postgresql-insert-an-array-of-composite-type-containing-arrays
CREATE TYPE WEIGHTEDREMEDY AS (REMEDY_ID INT, WEIGHT INT);

-- WORKS: insert into caserubric VALUES (  ROW(0, NULL, NULL, 0, 'FP', NULL, NULL), 'kent', 1, ARRAY[ROW(1,1)::WEIGHTEDREMEDY]  );
CREATE TYPE CASERUBRIC AS (
   RUBRIC_         RUBRIC,
   REPERTORYABBREV VARCHAR(40),
   RUBRICWEIGHT    INT,
   WEIGHTEDREMEDIES WEIGHTEDREMEDY[]
);

CREATE TABLE CAZE(
  ID           VARCHAR(255) NOT NULL,
  MEMBER_ID    INT NOT NULL,
  DATE_        DATE,
  DESCRIPTION  VARCHAR(1024),
  RESULTS      CASERUBRIC[] NOT NULL
);

# --- !Downs

DROP TABLE ACTIVITY;
DROP TABLE MEMBER;
DROP TYPE CASERUBRIC;
DROP TYPE WEIGHTEDREMEDY;
DROP TABLE CAZE;
DROP TABLE INFO;
DROP TABLE RUBRIC;
DROP TABLE REMEDY;

