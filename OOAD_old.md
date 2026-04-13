# OOAD Documentation

## 1. Creational Design Patterns

### Pattern Name: Singleton (Spring-managed Singleton Scope)

**Explanation (Project Context):**

* This project uses Spring IoC-managed singletons through stereotype/configuration annotations.
* Classes annotated with `@Service`, `@Controller`, `@Component`, and `@Configuration` are created once per application context by default.

**Why It Is Used:**

* Ensures one shared instance for stateless application services/controllers.
* Reduces object creation overhead and centralizes lifecycle management.

---

### Occurrences

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\MovieService.java`
* Class: `MovieService`

```java
@Service
public class MovieService {
```

**Explanation:**

* `MovieService` is managed as a singleton bean by Spring.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\RatingService.java`
* Class: `RatingService`

```java
@Service
public class RatingService {
```

**Explanation:**

* `RatingService` is a singleton service bean used across requests.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\PasswordHashService.java`
* Class: `PasswordHashService`

```java
@Service
public class PasswordHashService {
```

**Explanation:**

* One shared hashing service bean is reused by multiple controllers.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\filter\NoCacheFilter.java`
* Class: `NoCacheFilter`

```java
@Component
public class NoCacheFilter implements Filter {
```

**Explanation:**

* Filter is registered as a singleton Spring component.

#### Instance 5

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\config\DatabaseBootstrapConfiguration.java`
* Class: `DatabaseBootstrapConfiguration`

```java
@Configuration(proxyBeanMethods = false)
public class DatabaseBootstrapConfiguration {
```

**Explanation:**

* Configuration class is singleton in the application context.

#### Instance 6

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\config\DatabaseBootstrapConfiguration.java`
* Class: `DatabaseBootstrapConfiguration`

```java
@Bean
public FlywayMigrationStrategy flywayMigrationStrategy(CineBookDbProperties dbProperties) {
```

**Explanation:**

* Bean method contributes a singleton `FlywayMigrationStrategy` by default.

#### Instance 7

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\AdminController.java`
* Class: `AdminController`

```java
@Controller
public class AdminController {
```

**Explanation:**

* Controller is singleton-scoped in Spring MVC.

#### Instance 8

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\AuthController.java`
* Class: `AuthController`

```java
@Controller
public class AuthController {
```

**Explanation:**

* One controller instance handles all mapped auth requests.

#### Instance 9

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\BookingController.java`
* Class: `BookingController`

```java
@Controller
public class BookingController {
```

**Explanation:**

* Singleton MVC controller for booking flow endpoints.

#### Instance 10

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\CarouselController.java`
* Class: `CarouselController`

```java
@Controller
public class CarouselController {
```

**Explanation:**

* Singleton controller serving carousel image endpoints.

#### Instance 11

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\CatalogController.java`
* Class: `CatalogController`

```java
@Controller
public class CatalogController {
```

**Explanation:**

* Singleton controller for catalog and search endpoints.

#### Instance 12

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\MovieController.java`
* Class: `MovieController`

```java
@Controller
public class MovieController {
```

**Explanation:**

* Singleton controller for index/movie page rendering.

#### Instance 13

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\ProfileController.java`
* Class: `ProfileController`

```java
@Controller
public class ProfileController {
```

**Explanation:**

* Singleton controller for profile/history/password operations.

#### Instance 14

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\RatingController.java`
* Class: `RatingController`

```java
@RestController
public class RatingController {
```

**Explanation:**

* REST controller is also a singleton Spring bean.

#### Instance 15

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\GlobalUserModelAdvice.java`
* Class: `GlobalUserModelAdvice`

```java
@ControllerAdvice
public class GlobalUserModelAdvice {
```

**Explanation:**

* Advice component is singleton and shared globally across controllers.

---

### Pattern Name: Factory Method

**Explanation (Project Context):**

* The project uses static factory methods to centralize object creation logic for domain/view objects.

**Why It Is Used:**

* Avoids repeated construction logic in controllers/services.
* Keeps creation intent explicit (`registeredUser`, `createForCatalog`, `of`, `fromAvailability`).

---

### Occurrences

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\User.java`
* Class: `User`

```java
public static User registeredUser(String username, String email, String passwordHash) {
    return new User(username, email, passwordHash);
}
```

**Explanation:**

* Named factory method encapsulates creation of a registered user.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\Movie.java`
* Class: `Movie`

```java
public static Movie createForCatalog(String title,
                                     Integer durationMinutes,
                                     String language,
                                     String certification,
                                     String description,
                                     byte[] poster) {
    Movie movie = new Movie();
    movie.setTitle(title);
    movie.setDurationMinutes(durationMinutes);
    movie.setLanguage(language);
    movie.setCertification(certification);
    movie.setDescription(description);
    movie.setPoster(poster);
    return movie;
}
```

**Explanation:**

* Factory method creates and initializes `Movie` for admin catalog insertion.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\ShowSchedule.java`
* Class: `ShowSchedule`

```java
public static ShowSchedule of(Movie movie,
                              Theater theater,
                              LocalDate startDate,
                              LocalDate endDate,
                              LocalTime startTime,
                              Integer screen) {
    ShowSchedule schedule = new ShowSchedule();
    schedule.setMovie(movie);
    schedule.setTheater(theater);
    schedule.setStartDate(startDate);
    schedule.setEndDate(endDate);
    schedule.setStartTime(startTime);
    schedule.setScreen(screen);
    return schedule;
}
```

**Explanation:**

* Factory method captures consistent creation of show schedules.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\MovieController.java`
* Class: `MovieController.TierStatus`

```java
public static TierStatus fromAvailability(int bookedSeats, int totalSeats) {
    if (totalSeats <= 0) {
        return new TierStatus("Sold Out", "sold-out");
    }
    int remainingSeats = Math.max(0, totalSeats - Math.max(0, bookedSeats));
    if (remainingSeats == 0) {
        return new TierStatus("Sold Out", "sold-out");
    }
    double remainingRatio = remainingSeats / (double) totalSeats;
    if (remainingRatio <= 0.25d) {
        return new TierStatus("Filling Fast", "filling");
    }
    return new TierStatus("Available", "available");
}
```

**Explanation:**

* Factory method chooses which `TierStatus` object to create based on availability conditions.

#### Instance 5

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\PasswordHashService.java`
* Class: `PasswordHashService`

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
```

**Explanation:**

* Uses JDK Factory Method (`getInstance`) to obtain algorithm-specific hasher.

---

### Pattern Name: Builder

**Explanation (Project Context):**

* Builder is used for constructing `MovieBooking` with multiple fields in a fluent way.

**Why It Is Used:**

* Improves readability of complex object creation.
* Avoids partial object initialization spread across many setter calls.

---

### Occurrences

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\MovieBooking.java`
* Class: `MovieBooking`

```java
public static Builder builder() {
    return new Builder();
}

public static class Builder {
    private final MovieBooking booking = new MovieBooking();
```

**Explanation:**

* Exposes a dedicated builder entry point and builder type.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\MovieBooking.java`
* Class: `MovieBooking.Builder`

```java
public Builder show(ShowSchedule show) {
    booking.setShow(show);
    return this;
}

public Builder user(User user) {
    booking.setUser(user);
    return this;
}

public MovieBooking build() {
    return booking;
}
```

**Explanation:**

* Fluent setters return `Builder` and terminate with `build()`.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\MovieService.java`
* Class: `MovieService`

```java
MovieBooking booking = MovieBooking.builder()
        .show(schedule)
        .user(user)
        .showDate(showDate)
        .seatCount(boundedTicketCount)
        .totalPrice(calculateBookingTotal(selectedSeats, schedule.getTheater()))
        .seatNumbers(String.join(",", selectedSeats))
        .publicId(generatePublicId())
        .build();
```

**Explanation:**

* Real usage of the builder in production booking flow.

---

### Pattern Name: Abstract Factory

**Explanation (Project Context):**

* No abstract factory hierarchy exists in this codebase.

**Why It Is Used:**

* Not applicable.

---

### Occurrences

* No occurrences found.

---

### Pattern Name: Prototype

**Explanation (Project Context):**

* No clone-based object duplication (`Cloneable`/`clone`) is present.

**Why It Is Used:**

* Not applicable.

---

### Occurrences

* No occurrences found.

---

## 2. SOLID Principles

### Principle: Single Responsibility Principle (SRP)

---

### Instances

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\PasswordHashService.java`
* Class: `PasswordHashService`

```java
public class PasswordHashService {
    public String hashSha256(String input) {
        // hashing logic only
    }
}
```

**Explanation:**

* Class has one focused responsibility: password hashing.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\RatingService.java`
* Class: `RatingService`

```java
public MovieRating submitOrUpdateRating(User user, Movie movie, int ratingValue) {
    Optional<MovieRating> existing = movieRatingRepository.findByMovieIdAndUserId(movie.getId(), user.getId());
    MovieRating rating = existing.orElseGet(MovieRating::new);
    rating.setMovie(movie);
    rating.setUser(user);
    rating.setRating(ratingValue);
    return movieRatingRepository.save(rating);
}
```

**Explanation:**

* Handles only rating-related business rules and persistence orchestration.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\filter\NoCacheFilter.java`
* Class: `NoCacheFilter`

```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        http.setHeader("Pragma", "no-cache");
        http.setDateHeader("Expires", 0);
    }
    chain.doFilter(request, response);
}
```

**Explanation:**

* Sole responsibility is applying no-cache HTTP headers.

### Violations

#### Violation 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\AdminController.java`

```java
@PostMapping("/admin/banners")
public ResponseEntity<String> uploadAdminBanner(...)

@PostMapping("/admin/movies")
public ResponseEntity<String> addMovie(...)

@PostMapping("/admin/shows")
@Transactional
public ResponseEntity<String> addShowSchedule(...)
```

**Issue:**

* `AdminController` aggregates multiple domains (banner management, movie management, theater/schedule management).

**Minimal Fix Suggestion:**

* Extract small services (`BannerAdminService`, `ShowScheduleAdminService`) and keep controller as HTTP adapter.

---

### Principle: Open/Closed Principle (OCP)

---

### Instances

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRepository.java`
* Class: `MovieRepository`

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
```

**Explanation:**

* Behavior is extended by adding new query methods without modifying framework code.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\ShowScheduleRepository.java`
* Class: `ShowScheduleRepository`

```java
public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {
```

**Explanation:**

* Repository remains open to new query contracts while closed for core CRUD internals.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\config\DatabaseBootstrapConfiguration.java`
* Class: `DatabaseBootstrapConfiguration`

```java
@Bean
public FlywayMigrationStrategy flywayMigrationStrategy(CineBookDbProperties dbProperties) {
    return flyway -> {
        ensureDatabaseExists(dbProperties);
        flyway.migrate();
    };
}
```

**Explanation:**

* Uses strategy abstraction; migration behavior can be replaced by supplying a different strategy bean.

### Violations

#### Violation 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\MovieController.java`

```java
private static final int ELITE_SEAT_CAPACITY = 64;
private static final int NORMAL_SEAT_CAPACITY = 112;
```

**Issue:**

* Seat capacities are hardcoded; changing theater seat model requires code edits.

**Minimal Fix Suggestion:**

* Move capacities to configuration/properties or derive from theater metadata with defaults.

---

### Principle: Liskov Substitution Principle (LSP)

---

### Instances

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\filter\NoCacheFilter.java`
* Class: `NoCacheFilter`

```java
public class NoCacheFilter implements Filter {
```

**Explanation:**

* `NoCacheFilter` can be substituted wherever `Filter` is expected.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\UserRepository.java`
* Class: `UserRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {
```

**Explanation:**

* Can substitute `JpaRepository<User, Long>` contract in Spring Data infrastructure.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRepository.java`
* Class: `MovieRepository`

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
```

**Explanation:**

* Honors base repository contract; substitutable in generic repository operations.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieBookingRepository.java`
* Class: `MovieBookingRepository`

```java
public interface MovieBookingRepository extends JpaRepository<MovieBooking, Long> {
```

**Explanation:**

* Proper subtype of repository abstraction.

#### Instance 5

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRatingRepository.java`
* Class: `MovieRatingRepository`

```java
public interface MovieRatingRepository extends JpaRepository<MovieRating, Long> {
```

**Explanation:**

* Repository substitution through inheritance is preserved.

#### Instance 6

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\ShowScheduleRepository.java`
* Class: `ShowScheduleRepository`

```java
public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {
```

**Explanation:**

* Substitutable specialized repository implementation.

#### Instance 7

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\TheaterRepository.java`
* Class: `TheaterRepository`

```java
public interface TheaterRepository extends JpaRepository<Theater, Long> {
```

**Explanation:**

* Preserves base repository contract.

#### Instance 8

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\CarouselRepository.java`
* Class: `CarouselRepository`

```java
public interface CarouselRepository extends JpaRepository<CarouselImage, Long> {
```

**Explanation:**

* Substitutable with base `JpaRepository` type.

#### Instance 9

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\LoginAttemptRepository.java`
* Class: `LoginAttemptRepository`

```java
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
```

**Explanation:**

* Maintains substitutability with Spring Data repository abstraction.

---

### Principle: Interface Segregation Principle (ISP)

---

### Instances

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\UserRepository.java`
* Class: `UserRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
```

**Explanation:**

* Interface is narrowly focused on user-specific queries.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\TheaterRepository.java`
* Class: `TheaterRepository`

```java
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    Optional<Theater> findByName(String name);
    java.util.List<Theater> findTop10ByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrLocationContainingIgnoreCase(String name, String city, String location);
    @Query("SELECT DISTINCT t.city FROM Theater t ORDER BY t.city")
    java.util.List<String> findDistinctCities();
}
```

**Explanation:**

* Exposes only theater-centric query operations.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\CarouselRepository.java`
* Class: `CarouselRepository`

```java
interface CarouselProjection {
    Long getId();
    String getImageName();
}

interface BannerBinaryProjection {
    Long getId();
    byte[] getImageData();
    String getFileType();
    String getImageName();
}
```

**Explanation:**

* Segregated projection interfaces prevent clients from depending on unnecessary fields.

---

### Principle: Dependency Inversion Principle (DIP)

---

### Instances

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\AuthController.java`
* Class: `AuthController`

```java
@Autowired
private UserRepository userRepository;

@Autowired
private LoginAttemptRepository loginAttemptRepository;

@Autowired
private PasswordHashService passwordHashService;
```

**Explanation:**

* High-level auth flow depends on injected abstractions/beans, not manual instantiation.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\BookingController.java`
* Class: `BookingController`

```java
@Autowired
private ShowScheduleRepository showScheduleRepository;
@Autowired
private MovieBookingRepository movieBookingRepository;
@Autowired
private UserRepository userRepository;
@Autowired
private MovieService movieService;
```

**Explanation:**

* Booking flow relies on injected dependencies, not concrete object construction.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\RatingService.java`
* Class: `RatingService`

```java
@Autowired
private MovieRatingRepository movieRatingRepository;
```

**Explanation:**

* Service depends on repository abstraction and delegates persistence.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\config\DatabaseBootstrapConfiguration.java`
* Class: `DatabaseBootstrapConfiguration`

```java
public FlywayMigrationStrategy flywayMigrationStrategy(CineBookDbProperties dbProperties) {
```

**Explanation:**

* Depends on configuration abstraction (`CineBookDbProperties`) injected by Spring.

### Violations

#### Violation 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\RatingController.java`

```java
@Autowired
private RatingService ratingService;
```

**Issue:**

* Depends on concrete service class type instead of service interface.

**Minimal Fix Suggestion:**

* Introduce `RatingOperations` interface and inject that interface.

#### Violation 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\AuthController.java`

```java
@Autowired
private UserRepository userRepository;
```

**Issue:**

* Uses field injection, which tightly couples wiring style and weakens immutability/testability.

**Minimal Fix Suggestion:**

* Use constructor injection for all mandatory dependencies.

---

## 3. OOAD Concepts

### Concept: Encapsulation

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\User.java`
* Class: `User`

```java
private Long id;
private String username;
private String email;
private String passwordHash;

public String getUsername() { return username; }
public void setUsername(String username) { this.username = username; }
```

**Explanation**

* User state is hidden behind private fields and controlled via accessors.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\Movie.java`
* Class: `Movie`

```java
private String title;
private Integer durationMinutes;
private Double rating = 0.0;

public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }
```

**Explanation**

* Domain fields are encapsulated and manipulated through methods.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\MovieBooking.java`
* Class: `MovieBooking`

```java
private String publicId;
private OffsetDateTime createdAt;

@PrePersist
protected void onCreate() {
    if (createdAt == null) {
        createdAt = OffsetDateTime.now();
    }
    if (publicId == null || publicId.isBlank()) {
        publicId = UUID.randomUUID().toString();
    }
}
```

**Explanation**

* Internal lifecycle state is encapsulated and guarded before persistence.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\config\CineBookDbProperties.java`
* Class: `CineBookDbProperties`

```java
private String host = "localhost";
private int port = 5432;

public String getHost() { return host; }
public void setHost(String host) { this.host = host; }
```

**Explanation**

* Configuration details are encapsulated in a dedicated value object.

#### Instance 5

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\dto\PaymentRequest.java`
* Class: `PaymentRequest`

```java
private Long movieId;
private LocalDate showDate;
private Integer ticketCount;

public Long getMovieId() { return movieId; }
public void setMovieId(Long movieId) { this.movieId = movieId; }
```

**Explanation**

* Request data is encapsulated as a DTO with controlled accessors.

---

### Concept: Abstraction

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRepository.java`
* Class: `MovieRepository`

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitle(String title);
}
```

**Explanation**

* Repository interface abstracts data access implementation.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\MovieService.java`
* Class: `MovieService`

```java
public int calculateBookingTotal(List<String> seatNumbers, Theater theater) {
    int ticketPrice = resolveTicketPrice(theater);
    int eliteTicketPrice = resolveEliteTicketPrice(theater);
    // ...
}
```

**Explanation**

* Encapsulates booking price rules behind a service API.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\service\RatingService.java`
* Class: `RatingService`

```java
@Transactional(readOnly = true)
public double getAverageRating(Long movieId) {
    Double avg = movieRatingRepository.findAverageRatingByMovieId(movieId);
    return avg == null ? 0.0 : avg;
}
```

**Explanation**

* Business-level rating abstraction hides repository/query details.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\controller\GlobalUserModelAdvice.java`
* Class: `GlobalUserModelAdvice`

```java
public static String resolveProfileImageUrl(String profileImage) {
    if (profileImage == null || profileImage.isBlank()) {
        return DEFAULT_AVATAR;
    }
    // ...
}
```

**Explanation**

* Provides reusable abstraction for profile image URL resolution across views.

---

### Concept: Polymorphism

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\filter\NoCacheFilter.java`
* Class: `NoCacheFilter`

```java
public class NoCacheFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
```

**Explanation**

* Runtime polymorphism via interface implementation and method override.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\model\LoginAttempt.java`
* Class: `LoginAttempt`

```java
public LoginAttempt() {}

public LoginAttempt(String username, boolean success) {
    this(username, null, success);
}

public LoginAttempt(String username, Long userId, boolean success) {
    this.username = username;
    this.userId = userId;
    this.success = success;
}
```

**Explanation**

* Compile-time polymorphism through constructor overloading.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\UserRepository.java`
* Class: `UserRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {
```

**Explanation**

* Spring provides runtime proxy implementations for repository interfaces (interface polymorphism).

---

### Concept: Inheritance

#### Instance 1

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\UserRepository.java`
* Class: `UserRepository`

```java
public interface UserRepository extends JpaRepository<User, Long> {
```

**Explanation**

* Interface inheritance from `JpaRepository`.

#### Instance 2

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRepository.java`
* Class: `MovieRepository`

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
```

**Explanation**

* Inherits generic CRUD contract from parent interface.

#### Instance 3

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\ShowScheduleRepository.java`
* Class: `ShowScheduleRepository`

```java
public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {
```

**Explanation**

* Interface inheritance reused for schedule persistence abstraction.

#### Instance 4

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieBookingRepository.java`
* Class: `MovieBookingRepository`

```java
public interface MovieBookingRepository extends JpaRepository<MovieBooking, Long> {
```

**Explanation**

* Inherits data access operations from framework contract.

#### Instance 5

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\MovieRatingRepository.java`
* Class: `MovieRatingRepository`

```java
public interface MovieRatingRepository extends JpaRepository<MovieRating, Long> {
```

**Explanation**

* Inheritance used to specialize rating repository.

#### Instance 6

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\TheaterRepository.java`
* Class: `TheaterRepository`

```java
public interface TheaterRepository extends JpaRepository<Theater, Long> {
```

**Explanation**

* Parent interface behavior inherited for theater persistence.

#### Instance 7

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\CarouselRepository.java`
* Class: `CarouselRepository`

```java
public interface CarouselRepository extends JpaRepository<CarouselImage, Long> {
```

**Explanation**

* Inherits generic repository operations through interface inheritance.

#### Instance 8

* File: `D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\CineBook\src\main\java\com\CineBook\repository\LoginAttemptRepository.java`
* Class: `LoginAttemptRepository`

```java
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
```

**Explanation**

* Inheritance-based specialization for login attempt data access.

---

## Notes on Scope and Evidence

* This document is based on a full scan of `src/main/java` in the project.
* Only patterns/principles with direct code evidence are included.
* Abstract Factory and Prototype are intentionally marked absent because no concrete implementation exists in the codebase.
