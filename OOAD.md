# OOAD

## 1. Creational Design Patterns

### Singleton (Spring-managed)

**Explanation:** Spring-managed beans annotated with @Service,
@Controller, etc. are singleton-scoped by default.

**Note:** This is a framework-managed singleton, not a manual
implementation.

#### Instances

-   File: CineBook/service/MovieService.java

``` java
@Service
public class MovieService {
}
```

-   File: CineBook/controller/AuthController.java

``` java
@Controller
public class AuthController {
}
```

------------------------------------------------------------------------

### Factory Method (Static Factory Methods)

#### Instance 1

-   File: CineBook/model/User.java

``` java
public static User registeredUser(String username, String email, String passwordHash) {
    return new User(username, email, passwordHash);
}
```

#### Instance 2

-   File: CineBook/model/Movie.java

``` java
public static Movie createForCatalog(String title, Integer durationMinutes, String language,
                                     String certification, String description, byte[] poster) {
    Movie movie = new Movie();
    movie.setTitle(title);
    return movie;
}
```

------------------------------------------------------------------------

### Builder

#### Instance 1

-   File: CineBook/model/MovieBooking.java

``` java
public static Builder builder() {
    return new Builder();
}
```

#### Instance 2

-   File: CineBook/service/MovieService.java

``` java
MovieBooking booking = MovieBooking.builder()
        .show(schedule)
        .user(user)
        .build();
```

------------------------------------------------------------------------

## 2. SOLID Principles

### Single Responsibility Principle (SRP)

#### Instance

-   File: CineBook/service/PasswordHashService.java

``` java
public class PasswordHashService {
    public String hashSha256(String input) {
        // hashing logic only
    }
}
```

------------------------------------------------------------------------

### Open/Closed Principle (OCP)

#### Instance

-   File: CineBook/config/DatabaseBootstrapConfiguration.java

``` java
@Bean
public FlywayMigrationStrategy flywayMigrationStrategy(CineBookDbProperties dbProperties) {
    return flyway -> {
        ensureDatabaseExists(dbProperties);
        flyway.migrate();
    };
}
```

------------------------------------------------------------------------

### Dependency Inversion Principle (DIP)

#### Instance

-   File: CineBook/controller/BookingController.java

``` java
@Autowired
private MovieService movieService;
```

**Note:** Demonstrates dependency injection, but could be improved using
interfaces.

------------------------------------------------------------------------

## 3. OOP Concepts

### Encapsulation

#### Instance

-   File: CineBook/model/User.java

``` java
private String username;

public String getUsername() { return username; }
```

------------------------------------------------------------------------

### Abstraction

#### Instance

-   File: CineBook/service/MovieService.java

``` java
public int calculateBookingTotal(List<String> seatNumbers, Theater theater) {
    // logic hidden
}
```

------------------------------------------------------------------------

### Polymorphism

#### Instance

-   File: CineBook/filter/NoCacheFilter.java

``` java
public class NoCacheFilter implements Filter {
    @Override
    public void doFilter(...) {
    }
}
```
