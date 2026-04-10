# CineBook

CineBook is a movie ticket booking web application where users can browse movies, choose theaters and showtimes, and book seats online. It also includes an admin panel for managing movies, theaters, shows, and homepage banners.

## Features

- End-to-end user booking flow: select movie, theater, showtime, and seats.
- Booking confirmation with a shareable ticket link.
- Booking history for users.
- Ability for users to rate movies.
- Admin dashboard to manage movies, theaters, shows, and banners.
- Light/Dark mode toggle

## Tech Stack

- Backend: Spring Boot
- Architecture: Spring MVC
- Database: PostgreSQL
- ORM: Spring Data JPA
- Build Tool: Maven
- Frontend: HTML, CSS, JavaScript

## Setup Instructions

### Prerequisites

- Java 17+
- Maven
- PostgreSQL

### 1) Database Setup

Create a PostgreSQL database named `cinebook`.

```sql
CREATE DATABASE cinebook;
```

Update your database credentials in `src/main/resources/application-db.properties` and also set your local project root path:

```properties
cinebook.db.username=your_username
cinebook.db.password=your_password
cinebook.db.host=localhost
cinebook.db.port=5432
cinebook.db.database-name=cinebook
cinebook.db.project-root-path=C:\\path\\to\\your\\CineBook\\
```


### 2) Run the Project

```bash
mvn spring-boot:run
```

Open the app at:

```text
http://localhost:8080
```
