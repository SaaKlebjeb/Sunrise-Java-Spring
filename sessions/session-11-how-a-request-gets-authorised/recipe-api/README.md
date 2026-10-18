# Exercise — The Recipe API, secured with JWT

Same project as last session. Today the API learns to ask *who are you?* and *are you allowed?*
before it does anything. Anyone can still read recipes. To write, you **register**, **log in once**,
and get a signed **token** that every request carries. Deleting is for the head chef only.

**Java 25 · Spring Boot 4.1.1 · Maven · PostgreSQL · JJWT 0.13.0.**

**You need PostgreSQL running from the start**, with the same `recipes_db` as Session 10.

---

## Who you are building for today

**Sunrise Kitchen** is about to put the API online, and asked:

- *"Before we put this online, we need to know who's using it."*
- *"Anyone can browse recipes. To add or change one, you need an account."*
- *"People sign up themselves. Our head chef, Sokha, is the only admin."*
- *"Log in once on the phone — don't make the app keep the password."*
- *"Deleting a recipe is final. Only the head chef can do that."*

## Start here

**This folder has no code in it — only this file.** You bring the code: today continues the project
you finished last session.

1. Copy your Session 10 project into this folder — everything in
   `../../session-10-spring-data-liquibase-swagger/recipe-api/` **except** its `README.md` and
   `EXTRA-tests.md`. From this folder:

   ```bash
   cp -r ../../session-10-spring-data-liquibase-swagger/recipe-api/{pom.xml,src,mvnw,mvnw.cmd,.mvn} .
   ```

   On Windows, copy `pom.xml`, `src`, `mvnw`, `mvnw.cmd` and `.mvn` across in File Explorer.
2. In the copied `pom.xml`, change `<artifactId>recipe-api-liquibase</artifactId>` to
   `<artifactId>recipe-api-security</artifactId>`, so IntelliJ does not confuse it with last
   session's project.
3. **Open the `pom.xml`, not the folder**, when you import it into IntelliJ.
4. Run `mvn test` and start the application once before you change anything:

   ```bash
   curl -i http://localhost:8080/recipes
   ```

   `200 OK`: Fish Amok, and whatever you added in Session 10. **If this fails, fix it now.**

Your Session 10 folder stays as it is. Everything today goes into the copy.

**Today builds on all of last session.** Before you start, your project should have:

| From last session | Used today in |
|---|---|
| PostgreSQL `recipes_db`, Liquibase changesets `001`–`003`, `ddl-auto=validate` | Checkpoint 3 adds changeset `004` for the users |
| Tags — `TagService` (interface) + `TagServiceImpl`, and `TagController` with `PUT /recipes/{id}/tags/{name}` and two `GET`s | Checkpoint 2 opens the `GET`s and protects the `PUT`. The tag classes themselves do not change today |
| `RecipeServiceImpl.delete(id)` with `@Transactional` | Checkpoint 6 puts the head chef's rule on it |
| `RecipeMapper` — MapStruct, `componentModel = SPRING` | Checkpoint 3 adds a second mapper, `UserMapper`, in the same shape |
| `GlobalExceptionHandler` with specific handlers only, no catch-all | Checkpoints 3, 4 and 5 add one handler each |

If one of them is missing, finish it first.

### What you will add today

```
com.sunrise.recipeapi
├── domain/        + Role, AppUser                              (checkpoint 3)
├── repository/    + AppUserRepository                          (checkpoint 3)
├── mapper/        + UserMapper                                 (checkpoint 3)
├── security/      + SecurityConfig                             (checkpoint 2, grows in 3–6)
│                  + CustomUserDetailsService                   (checkpoint 3)
│                  + JwtService                                 (checkpoint 4)
│                  + JwtAuthenticationFilter                    (checkpoint 5)
├── service/       + AuthService, AuthServiceImpl, EmailAlreadyExistsException   (3, grows in 4)
│                  + UserService, UserServiceImpl                                (checkpoint 5, reads the SecurityContext)
├── controller/    + AuthController                             (checkpoint 3, grows in 4)
│                  + UserController                             (checkpoint 5)
├── dto/           + RegisterRequest, UserResponse              (checkpoint 3)
│                  + LoginRequest, AuthResponse                 (checkpoint 4)
└── resources/db/changelog/   + 004-create-users.sql            (checkpoint 3)
```

`security` is a new package, at the same level as `controller` and `service`. Everything else goes
where it has gone since Session 7: the controller speaks HTTP, the service holds the rule, the
mapper turns entities into DTOs.

---

## What you check today

**`mvn test` stays green at the end of every checkpoint.** `contextLoads` starts the whole
application — security included — so PostgreSQL must be running.

| Checkpoint | What you do | How you check it |
|---|---|---|
| 1 | Add the starter, change nothing else | `curl` — every endpoint is `401` |
| 2 | Your own rules in `SecurityConfig` | `GET` is `200`, an anonymous `POST` is `401` · one deliberate break |
| 3 | Users in PostgreSQL, BCrypt, `POST /auth/register` | `201`, then `409` · hashes in the `users` table |
| 4 | A signed JWT from `POST /auth/login` | a token comes back · read it on jwt.io |
| 5 | The filter that accepts the token, and `GET /me` | `/me` answers for Dara · two deliberate breaks |
| 6 | Only the head chef deletes — on the URL first, then on the service | `401`, `403`, `204` · one deliberate break |

No new tests are written in class. The lecture's *Extra learning material* shows how to test these
rules with `@WithMockUser` — for afterwards, if you want it.

### Pace

Checkpoints 3 and 4 are the longest: they are most of today's new files. Checkpoint 6 is short, and
it is the one that makes the session's point — do not skip it to finish 5.

---

## Checkpoint 1 — Lock the door

**What the client asked for:** *"Before we put this online, we need to know who's using it."*

You are not writing any security code yet. You add one dependency and watch what it does.

### The starter

In `pom.xml`, next to your other starters:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Your turn

1. Restart, and read the log:

   ```
   Using generated security password: 3f1c9a2e-....-....

   This generated password is for development use only. Your security configuration must be
   updated before running your application in production.
   ```

2. Try the API:

   ```bash
   curl -i http://localhost:8080/recipes
   # -> 401   WWW-Authenticate: Basic realm="Realm"

   curl -i -u user:PASTE-PASSWORD-HERE http://localhost:8080/recipes
   # -> 200
   ```

   You configured nothing, and every endpoint is locked. Spring Boot saw Spring Security on the
   classpath and installed a default setup — the same way it started Tomcat for the web starter.

3. That `-u user:password` is **HTTP Basic**: the username and password travel on **every**
   request. It is the default, it works, and it is what you are about to replace. A phone app would
   have to keep the password to send it each time. Today's API hands out a **token** instead.
4. `mvn test` — **green.**

---

## Checkpoint 2 — Your own rules

**What the client asked for:** *"Anyone can browse recipes. To add or change one, you need an
account."*

### The configuration

Create `security/SecurityConfig.java`:

```java
package com.sunrise.recipeapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // A JSON API: no login form, no session cookie, so no CSRF token
        .csrf(csrf -> csrf.disable())
        // Nothing is remembered between requests: the token is the session
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // The rules, read from top to bottom, first match wins
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
            .requestMatchers("/auth/**", "/error").permitAll()
            .anyRequest().authenticated())
        // Not signed in -> 401, not Spring's default 403
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    return http.build();
  }
}
```

| Line | Why |
|---|---|
| `csrf.disable()` | CSRF protection defends **browsers that carry a session cookie**. This API has neither, so it would only reject our `POST`s |
| `STATELESS` | Spring never creates an HTTP session. Every request proves itself — with a token, from checkpoint 5 |
| `GET /recipes/**`, `/tags/**` → `permitAll()` | Browsing stays open. That covers `/recipes/{id}`, `/search`, `/page`, `/filter`, `/recipes/{id}/tags` and `/tags/{name}/recipes` |
| `/auth/**` → `permitAll()` | Register and login must work **before** you have a token |
| `/error` → `permitAll()` | Lets Spring Boot's error page answer with the real status |
| `anyRequest().authenticated()` | Everything else needs a signed-in user. **Last**, as the safety net |
| `HttpStatusEntryPoint(UNAUTHORIZED)` | What to answer when someone is not signed in |

There is **no `.httpBasic()`** any more. Basic is gone.

> **Added Swagger after Session 10?** Also permit `"/v3/api-docs/**"` and `"/swagger-ui/**"`.

### Your turn

1. `mvn test` — **green.**
2. Restart, and check both halves of the client's sentence:

   ```bash
   curl -i http://localhost:8080/recipes
   # -> 200   open again

   curl -i -H "Content-Type: application/json" \
     -d '{"title":"Num banh chok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["rice noodles","fish curry"]}' \
     http://localhost:8080/recipes
   # -> 401   writes need a signed-in user — and there is no way to sign in yet
   ```

### Break it on purpose — the missing 401

Comment out the whole `.exceptionHandling(...)` line, restart, and send the same `POST`.

**Expected:** `403 Forbidden`, not `401`.

With no login method configured, Spring Security does not know how to ask anyone to sign in, so
its default is a flat 403. That is the wrong answer: **401 means "I don't know who you are", 403
means "I know who you are, and no"**. Nobody is signed in, so it must be 401. **Put the line back.**

---

## Checkpoint 3 — Users, and signing up

**What the client asked for:** *"People sign up themselves. Our head chef, Sokha, is the only
admin."*

`POST /auth/register` takes an email, a password and a name, and answers `201` with the new user —
without the password. A taken email is a `409`.

### The changeset

Your project has three changesets from Session 10. **Do not edit any of them.** Create
`src/main/resources/db/changelog/004-create-users.sql`:

```sql
--liquibase formatted sql

--changeset sunrise:004-create-users
create table users (
    id            bigint generated by default as identity primary key,
    email         varchar(100) not null unique,
    password_hash varchar(100) not null,
    full_name     varchar(100) not null,
    role          varchar(20)  not null
);
--rollback drop table users;

--changeset sunrise:004-seed-admin
-- The head chef. Nobody can register as ADMIN, so the first one is seeded. Password: chef1234
insert into users (email, password_hash, full_name, role)
values ('sokha@sunrisekitchen.com',
        '$2a$10$AXn5vDlOtZbox9gVlg9CqeuBjZi2XHLi8vLBmkMaqv1Zz5M97LF1y',
        'Sokha Chan', 'ADMIN');
--rollback delete from users where email = 'sokha@sunrisekitchen.com';
```

and add it at the bottom of `db.changelog-master.yaml`:

```yaml
  - include:
      file: 'db/changelog/004-create-users.sql'
```

That long string is a **BCrypt hash** of `chef1234`. The password itself is stored nowhere.

### The entity

`domain/Role.java`:

```java
package com.sunrise.recipeapi.domain;

// Stored as text in users.role: USER or ADMIN, without the ROLE_ prefix.
public enum Role {
  USER,
  ADMIN
}
```

`domain/AppUser.java` — the table's row, **and** the user Spring Security works with:

```java
package com.sunrise.recipeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  // BCrypt hash, never the password itself
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.USER;

  // Spring Security calls the login name "username". Ours is the email.
  @Override
  public String getUsername() {
    return email;
  }

  // The stored hash -- Spring compares it with BCrypt, never you
  @Override
  public String getPassword() {
    return passwordHash;
  }

  // A role is an authority with the ROLE_ prefix. Written by hand here: the enum holds ADMIN
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }
}
```

| Choice | Why |
|---|---|
| `AppUser`, not `User` | Spring Security already has a class called `User` — one wrong import away. (`user` is also a reserved word in PostgreSQL — hence the table is `users`) |
| `@Enumerated(EnumType.STRING)` | Stores `"ADMIN"`, not `1`. Add a role in the middle of the enum one day, and numbers would shift under your existing users |
| `implements UserDetails` | `UserDetails` is how Spring Security asks *"who is this user?"* — a login name, a stored hash, a list of authorities. Three methods answer it. The other four (`isEnabled`, `isAccountNonLocked`, …) have defaults that say `true` |
| `getUsername()` returns the email | Spring Security calls the login name "username". Ours is the email |
| `"ROLE_" + role.name()` | `hasRole("ADMIN")` looks for the authority `ROLE_ADMIN`. The enum holds `ADMIN`, so the prefix is added here — **once, in this one place** |

### The repository

`repository/AppUserRepository.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByEmail(String email);

  boolean existsByEmail(String email);
}
```

Two derived queries, written from their names — Session 9's idea.

### Where Spring Security finds a user

`security/CustomUserDetailsService.java`:

```java
package com.sunrise.recipeapi.security;

import com.sunrise.recipeapi.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final AppUserRepository appUserRepository;

  // AppUser implements UserDetails, so the entity itself is the answer
  @Override
  public UserDetails loadUserByUsername(String email) {
    return appUserRepository
        .findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("user email: " + email + " not found"));
  }
}
```

Spring Security asks one question — *"give me everything you know about this user"* — through the
`UserDetailsService` interface. You answer it from the `users` table. Because `AppUser` **is** a
`UserDetails`, the row you load is the answer: no copying into another class. From checkpoint 5 on,
this same `AppUser` is what sits in the `SecurityContext` for the whole request.

### A password encoder

Add to `SecurityConfig`:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
```

```java
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
```

### The DTOs and the mapper

`dto/RegisterRequest.java` — what the client sends:

```java
package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "email can not be blank") @Email(message = "email must be an email")
        String email,
    @NotBlank(message = "password can not be blank")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,
    @NotBlank(message = "fullName can not be blank") String fullName) {}
```

`dto/UserResponse.java` — what comes back. **No password hash in it**, ever:

```java
package com.sunrise.recipeapi.dto;

public record UserResponse(Long id, String email, String fullName, String role) {}
```

`mapper/UserMapper.java` — the same shape as your `RecipeMapper`:

```java
package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.RegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    // role is an enum; MapStruct writes its name() into the String
    UserResponse toUserResponse(AppUser user);

    // The service hashes the password and sets the role -- the request decides neither
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    // getAuthorities() is worked out from role -- there is nothing to copy into it
    @Mapping(target = "authorities", ignore = true)
    AppUser toAppUser(RegisterRequest request);
}
```

`toAppUser` copies `email` and `fullName`. It leaves `passwordHash` empty **on purpose**: a password
the client typed is not a hash, and copying it into that field would store it in plain text the
day someone forgets to encode it.

### The service

`service/EmailAlreadyExistsException.java`:

```java
package com.sunrise.recipeapi.service;

public class EmailAlreadyExistsException extends RuntimeException {
    EmailAlreadyExistsException(String message) {
        super(message);
    }
}
```

`service/AuthService.java` — one method for now; checkpoint 4 adds `login`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;

public interface AuthService {
    /**
     * Create an account with the USER role
     * Throws EmailAlreadyExistsException if the email is taken
     * @param user the new user, without a password hash yet
     * @param password the password as the user typed it; only its hash is stored
     * @return the saved user
     */
    AppUser register(AppUser user, String password);
}
```

`service/AuthServiceImpl.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.domain.Role;
import com.sunrise.recipeapi.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public AppUser register(AppUser user, String password) {
    if (appUserRepository.existsByEmail(user.getEmail())) {
      throw new EmailAlreadyExistsException("email: " + user.getEmail() + " already registered");
    }
    // Hash before it ever reaches the database
    user.setPasswordHash(passwordEncoder.encode(password));
    // Nobody registers as ADMIN -- whatever the request said
    user.setRole(Role.USER);
    return appUserRepository.save(user);
  }
}
```

**Three rules, and all three are the service's:** the email must be free, the password is hashed,
the role is always `USER`. That is why Sokha came from the changeset — nobody can register as
`ADMIN`.

### The controller

`controller/AuthController.java`:

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.RegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;
import com.sunrise.recipeapi.mapper.UserMapper;
import com.sunrise.recipeapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final UserMapper userMapper;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Validated @RequestBody RegisterRequest request) {
    AppUser savedUser = authService.register(userMapper.toAppUser(request), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toUserResponse(savedUser));
  }
}
```

The same three steps as `createRecipe`: the mapper turns the request into an entity, the service
saves it, the mapper turns the saved entity into the response.

### A taken email is a 409

Add to `GlobalExceptionHandler`:

```java
import com.sunrise.recipeapi.service.EmailAlreadyExistsException;
```

```java
  @ExceptionHandler(EmailAlreadyExistsException.class)
  ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getLocalizedMessage());
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/EmailAlreadyExists"));
    problemDetail.setTitle("Email already registered");
    return problemDetail;
  }
```

### Your turn

1. `mvn test` — **green.**
2. Restart. Liquibase runs **only the new changeset** — the table, then Sokha. And the generated
   password has **gone** from the log: you defined a `UserDetailsService`, so Spring Boot stopped
   making its default user.
3. Register Dara:

   ```bash
   curl -i -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"cook1234","fullName":"Dara Sok"}' \
     http://localhost:8080/auth/register
   # -> 201 {"id":2,"email":"dara@sunrisekitchen.com","fullName":"Dara Sok","role":"USER"}
   ```

4. Send the same request again:

   ```
   409 "email: dara@sunrisekitchen.com already registered"
   ```

5. Send a bad one:

   ```bash
   curl -i -H "Content-Type: application/json" \
     -d '{"email":"not-an-email","password":"123","fullName":""}' \
     http://localhost:8080/auth/register
   # -> 400   "3 fields not valid" — your Session 7 validation handler, for free
   ```

6. Look at the table:

   ```bash
   psql -d recipes_db -c "select email, password_hash, role from users;"
   ```

   Two rows, two hashes. Register a second user with the **same** password `cook1234` — the hash is
   **different**. BCrypt mixes a random salt into every one.

---

## Checkpoint 4 — Log in once, get a token

**What the client asked for:** *"Log in once on the phone — don't make the app keep the password."*

`POST /auth/login` takes an email and a password, checks them **once**, and answers with a signed
token the app keeps instead of the password.

### The dependencies

In `pom.xml`, add a version to `<properties>`:

```xml
<jjwt.version>0.13.0</jjwt.version>
```

and the three dependencies:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

You write code against `jjwt-api`. The other two do the work at run time: `impl` signs and checks,
`jackson` turns the claims into JSON.

### The properties

Add to `application.properties`:

```properties
# The key that signs every token. The default is for your laptop only;
# anywhere else it comes from the APP_JWT_SECRET environment variable.
app.jwt.secret=${APP_JWT_SECRET:dev-only-secret-change-me-0123456789abcdef}
# 15 minutes
app.jwt.expiration-ms=900000
```

**At least 32 characters.** HS256 needs a 256-bit key; a shorter one stops the app at startup with
`WeakKeyException`.

### `JwtService` — make a token, read a token

`security/JwtService.java`:

```java
package com.sunrise.recipeapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey signingKey;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    // HS256 needs at least 32 bytes of key
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(UserDetails user) {
    String role = user.getAuthorities().iterator().next().getAuthority();
    Date now = new Date();
    return Jwts.builder()
        .issuer("recipe-api")
        .subject(user.getUsername())
        .claim("role", role)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(signingKey)
        .compact();
  }

  // Checks the signature and the expiry; throws JwtException if either is wrong
  public String extractUsername(String token) {
    return parse(token).getSubject();
  }

  public long expirationSeconds() {
    return expirationMs / 1000;
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
```

| In the token | Holds |
|---|---|
| `iss` | `recipe-api` — who made it |
| `sub` | the user's email — who it is about |
| `role` | `ROLE_USER` or `ROLE_ADMIN` |
| `iat`, `exp` | when it was made, when it stops working |

`extractUsername` does more than its name says: `parseSignedClaims` **checks the signature and the
expiry first**, and throws if either is wrong.

### An `AuthenticationManager` for the login endpoint

Until now Spring Security checked passwords inside its own filters. The login endpoint checks one
**in your code**, so it needs that machinery as beans. Add to `SecurityConfig` — a field, and two
beans:

```java
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
```

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final UserDetailsService userDetailsService;
```

```java
  // Finds the user, checks the password with BCrypt -- used by /auth/login
  @Bean
  AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
```

and one more line at the end of the chain. The `;` moves from the end of the
`.exceptionHandling(...)` line to this one:

```java
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authenticationProvider(authenticationProvider());
    return http.build();
```

The **provider** knows how: load the user with your `CustomUserDetailsService`, compare with
BCrypt. The **manager** is the front door your service calls. The lecture's diagram *How the login pieces fit* shows
the whole path — manager → provider → `UserDetailsService` and `PasswordEncoder` — in one picture.

### The DTOs

`dto/LoginRequest.java`:

```java
package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "email can not be blank") String email,
    @NotBlank(message = "password can not be blank") String password) {}
```

`dto/AuthResponse.java`:

```java
package com.sunrise.recipeapi.dto;

public record AuthResponse(String token, String tokenType, long expiresIn, String email) {}
```

### The service

**`service/AuthService.java` — add one method:**

```java
    /**
     * Check the password once, and hand out a signed token
     * Wrong email or password throws BadCredentialsException
     * @param email the email the user logs in with
     * @param password the password as the user typed it
     * @return the token, its type and its lifetime
     */
    AuthResponse login(String email, String password);
```

New import: `com.sunrise.recipeapi.dto.AuthResponse`.

**Replace `service/AuthServiceImpl.java`** — two more fields, and `login`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.domain.Role;
import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.repository.AppUserRepository;
import com.sunrise.recipeapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  @Override
  @Transactional
  public AppUser register(AppUser user, String password) {
    if (appUserRepository.existsByEmail(user.getEmail())) {
      throw new EmailAlreadyExistsException("email: " + user.getEmail() + " already registered");
    }
    // Hash before it ever reaches the database
    user.setPasswordHash(passwordEncoder.encode(password));
    // Nobody registers as ADMIN -- whatever the request said
    user.setRole(Role.USER);
    return appUserRepository.save(user);
  }

  @Override
  public AuthResponse login(String email, String password) {
    // Wrong email or password -> BadCredentialsException -> 401
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email, password));
    UserDetails user = (UserDetails) authentication.getPrincipal();
    return new AuthResponse(
        jwtService.generateToken(user), "Bearer", jwtService.expirationSeconds(), user.getUsername());
  }
}
```

Why does `login` return a DTO, when `register` returns an entity? **There is no entity to return.**
A token is not kept in any table — it is made, signed and handed over. `AuthResponse` *is* the
result.

### The endpoint

**`controller/AuthController.java` — add:**

```java
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request.email(), request.password()));
  }
```

New imports: `com.sunrise.recipeapi.dto.AuthResponse` and `com.sunrise.recipeapi.dto.LoginRequest`.

### A wrong password is a 401, not a 500

The `AuthenticationManager` throws when the password is wrong, and it now comes out of **your**
controller. Add to `GlobalExceptionHandler`:

```java
import org.springframework.security.core.AuthenticationException;
```

```java
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleBadLogin(AuthenticationException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Wrong email or password");
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/BadCredentials"));
    problemDetail.setTitle("Login failed");
    return problemDetail;
  }
```

A wrong password and an unknown email get **the same** answer. If the API said "no such user" for
one, anyone could check which emails have accounts.

### Your turn

1. `mvn test` — **green.**
2. Log in as Dara:

   ```bash
   curl -i -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"cook1234"}' \
     http://localhost:8080/auth/login
   # -> 200 {"token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3Mi...","tokenType":"Bearer","expiresIn":900,"email":"dara@sunrisekitchen.com"}
   ```

3. And with a wrong password:

   ```bash
   curl -i -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"wrong"}' \
     http://localhost:8080/auth/login
   # -> 401 "Wrong email or password"
   ```

4. Paste the token into <https://jwt.io>. You can read every claim — nobody needed the secret for
   that:

   ```json
   {"alg":"HS256"}
   {"iss":"recipe-api","sub":"dara@sunrisekitchen.com","role":"ROLE_USER","iat":1792257025,"exp":1792257925}
   ```

5. Now try to **use** it:

   ```bash
   curl -i -H "Authorization: Bearer eyJhbGciOi..." -H "Content-Type: application/json" \
     -d '{"title":"Num banh chok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["rice noodles","fish curry"]}' \
     http://localhost:8080/recipes
   # -> 401
   ```

   The API hands out tokens, but nothing reads them yet. That is checkpoint 5.

---

## Checkpoint 5 — Accept the token

**What the client asked for:** *"So — the app sends that token, and it's in. And the app needs to
ask who is signed in, to show their name."*

Every request that carries `Authorization: Bearer <token>` is treated as that user. `GET /me`
answers with the signed-in user.

### The filter

`security/JwtAuthenticationFilter.java`:

```java
package com.sunrise.recipeapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    // No token: carry on as anonymous. The URL rules decide what happens next.
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);
    try {
      String email = jwtService.extractUsername(token);
      UserDetails user = userDetailsService.loadUserByUsername(email);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (JwtException | UsernameNotFoundException e) {
      // Forged, expired, or for a user who no longer exists: stay anonymous
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
```

Read it top to bottom — it is the whole idea of this session in one method:

| Step | What happens |
|---|---|
| No `Bearer` header? | Carry on as anonymous. The URL rules decide: a `GET` is fine, a `POST` gets 401 |
| Check the token | `extractUsername` verifies the signature and the expiry |
| Load the user | so a deleted user, or a changed role, counts at once |
| Put the user in the `SecurityContext` | Every filter after this one, `@PreAuthorize`, and your controller read the user from there |
| Forged or expired? | Stay anonymous — the rules turn that into 401 |

It extends `OncePerRequestFilter` so it runs exactly once per request.

### Put it in the chain

**Replace `security/SecurityConfig.java`** with this version:

```java
package com.sunrise.recipeapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // A JSON API: no login form, no session cookie, so no CSRF token
        .csrf(csrf -> csrf.disable())
        // Nothing is remembered between requests: the token is the session
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // The rules, read from top to bottom, first match wins
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
            .requestMatchers("/auth/**", "/error").permitAll()
            .anyRequest().authenticated())
        // Not signed in -> 401, not Spring's default 403
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authenticationProvider(authenticationProvider())
        // Read the Bearer token before Spring's own login filter would run
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Finds the user, checks the password with BCrypt -- used by /auth/login
  @Bean
  AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
```

New since checkpoint 4: the `JwtAuthenticationFilter` field, and `.addFilterBefore(...)` — *run my
filter before Spring's own username-and-password filter*, so the user is known by the time the rules
are checked.

### `GET /me` — the service reads the `SecurityContext`

The filter put the signed-in user in the `SecurityContext`. Anyone can read it from there, with one
line — `SecurityContextHolder.getContext().getAuthentication()`. So the **service** reads it, finds
the user in the database, and the controller stays one line, like every other endpoint.

`service/UserService.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;

public interface UserService {
    /**
     * Get the signed-in user
     * Reads the AppUser the JWT filter put in the SecurityContext
     * @return the signed-in user
     */
    AppUser getCurrentUser();
}
```

`service/UserServiceImpl.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Override
  public AppUser getCurrentUser() {
    // The Authentication the JWT filter put in the SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // Its principal is the UserDetails the filter loaded -- our own AppUser
    return (AppUser) authentication.getPrincipal();
  }
}
```

No repository, no second query. The filter loaded Dara from the database at the start of this
request — through your `CustomUserDetailsService` — and put that `AppUser` in the `SecurityContext`
as the **principal**. The service takes it back out.

`controller/UserController.java`:

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.dto.UserResponse;
import com.sunrise.recipeapi.mapper.UserMapper;
import com.sunrise.recipeapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final UserMapper userMapper;

  // Who is signed in? The service knows -- the controller only speaks HTTP
  @GetMapping("/me")
  public ResponseEntity<UserResponse> me() {
    return ResponseEntity.ok(userMapper.toUserResponse(userService.getCurrentUser()));
  }
}
```

| Layer | Its part in `GET /me` |
|---|---|
| Controller | HTTP only: the URL, the `200`, the mapper. No security code at all |
| Service | Asks the `SecurityContext` *who is signed in?* — the answer is the `AppUser` the filter loaded |
| Mapper | `AppUser` → `UserResponse`, the same method `/auth/register` uses. The hash never leaves |

**Why does this work from a service?** `SecurityContextHolder` keeps one `SecurityContext` **per
request thread**. Your filter filled it at the start of this request, and the controller and the
service run on the same thread, so they see the same user. Another request on another thread sees
its own.

**The price.** `getCurrentUser()` only has an answer **inside a signed-in request**. Call it from a
scheduled job, or a test that never signed anyone in, and `getAuthentication()` is `null`. In a test,
`@WithUserDetails("dara@sunrisekitchen.com")` loads the real `AppUser` for you; `@WithMockUser` puts
Spring's own `User` there instead, and the cast fails.

`/me` gets no rule of its own: `anyRequest().authenticated()` covers it.

### Your turn

1. `mvn test` — **green.**
2. Log in as each user and keep the tokens:

   ```bash
   curl -s -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"cook1234"}' http://localhost:8080/auth/login
   curl -s -H "Content-Type: application/json" \
     -d '{"email":"sokha@sunrisekitchen.com","password":"chef1234"}' http://localhost:8080/auth/login
   ```

   ```bash
   DARA=eyJhbGciOiJIUzI1NiJ9....
   SOKHA=eyJhbGciOiJIUzI1NiJ9....
   ```

   > **On Windows `cmd`**, use `set DARA=...` and `%DARA%`. In PowerShell, `$DARA="..."`. Or use
   > Postman: *Authorization → Bearer Token*.

3. Ask who you are:

   ```bash
   curl -i -H "Authorization: Bearer $DARA" http://localhost:8080/me
   # -> 200 {"id":2,"email":"dara@sunrisekitchen.com","fullName":"Dara Sok","role":"USER"}

   curl -i http://localhost:8080/me
   # -> 401
   ```

4. Create a recipe as Dara, and note the `id` it answers with — it is `<id>` below:

   ```bash
   curl -i -H "Authorization: Bearer $DARA" -H "Content-Type: application/json" \
     -d '{"title":"Test soup","cuisine":"khmer","minutes":10,"servings":1,"ingredients":["water"]}' \
     http://localhost:8080/recipes
   # -> 201   Location: /recipes/<id>
   ```

### Break it on purpose — forge a token

On jwt.io, paste Dara's token, change `ROLE_USER` to `ROLE_ADMIN` in the payload box, copy the new
token from the left, and use it:

```bash
curl -i -H "Authorization: Bearer <the edited token>" http://localhost:8080/me
# -> 401
```

In the filter, `extractUsername` threw `SignatureException: JWT signature does not match locally
computed signature`. Anyone can **read** a JWT. Only the holder of the secret can **write** one.

### Break it on purpose — watch it expire

Set `app.jwt.expiration-ms=30000`, restart, log in, and call `/me` with the new token. After 30
seconds: `401`. The filter caught `ExpiredJwtException`. **Set it back to `900000`.**

---

## Checkpoint 6 — Only the head chef deletes

**What the client asked for:** *"Deleting a recipe is final. Only the head chef can do that."*

`DELETE /recipes/{id}` answers `401` to nobody, `403` to Dara, and `204` to Sokha. You write the rule
twice — first where you already write rules, on the URL, then where it belongs.

### A rule on the URL

In `SecurityConfig`, add one line to the rules, **above** `anyRequest()`:

```java
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
            .requestMatchers("/auth/**", "/error").permitAll()
            // Only the head chef deletes
            .requestMatchers(HttpMethod.DELETE, "/recipes/**").hasRole("ADMIN")
            .anyRequest().authenticated())
```

`hasRole("ADMIN")` means *signed in **and** holding `ROLE_ADMIN`*. First match wins, so the line
must sit above `anyRequest()` — below it, `authenticated()` would answer first and Dara would get
through.

### Your turn

1. `mvn test` — **green.**
2. Delete the throwaway recipe from checkpoint 5, three times:

   ```bash
   curl -i -X DELETE http://localhost:8080/recipes/<id>
   # -> 401   nobody signed in

   curl -i -X DELETE -H "Authorization: Bearer $DARA"  http://localhost:8080/recipes/<id>
   # -> 403   signed in, but not an admin

   curl -i -X DELETE -H "Authorization: Bearer $SOKHA" http://localhost:8080/recipes/<id>
   # -> 204
   ```

   **401: "I don't know who you are." 403: "I know who you are, and the answer is no."**

### Break it on purpose — a second door

> **The client, a month later:** *"The mobile team wants a 'remove' button. Same thing as deleting,
> really."*

Somebody adds this to `RecipeController`:

```java
  // The mobile app's "remove" button -- it deletes the recipe too
  @PostMapping("/{id}/remove")
  public ResponseEntity<Void> removeRecipe(@PathVariable Long id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();
  }
```

Restart, create another throwaway recipe as Dara (a new `<id>`), and remove it **as Dara**:

```bash
curl -i -X POST -H "Authorization: Bearer $DARA" http://localhost:8080/recipes/<id>/remove
# -> 204   Dara just deleted a recipe
```

Nothing is broken. The URL rule protects `DELETE /recipes/**`, exactly as written. This is a
`POST`, to another URL, so `anyRequest().authenticated()` lets any signed-in user in — and the
controller calls the same `delete`. **The rule was on a door. The client's rule is about the
action.**

### The rule moves to the action

The action is `RecipeServiceImpl.delete`, and every door leads to it. Put the rule there.

1. In `SecurityConfig`, **remove** the `DELETE` line you added — the rule is about to have one
   home, not two.
2. Switch method security on, on the class:

   ```java
   import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
   ```

   ```java
   @Configuration
   @EnableMethodSecurity
   @RequiredArgsConstructor
   public class SecurityConfig {
   ```

3. In `RecipeServiceImpl`:

   ```java
   import org.springframework.security.access.prepost.PreAuthorize;
   ```

   ```java
     @Override
     @Transactional
     @PreAuthorize("hasRole('ADMIN')")
     public void delete(Long id) {
       recipeRepository.deleteById(id);   // unchanged
     }
   ```

The same expression, `hasRole('ADMIN')`, now guards the method instead of a URL.

### Your turn

1. `mvn test` — **green.**
2. Restart, create one throwaway recipe as Dara, and try both doors:

   ```bash
   curl -i -X POST -H "Authorization: Bearer $DARA" http://localhost:8080/recipes/<id>/remove
   # -> 403   the second door is shut too

   curl -i -X DELETE http://localhost:8080/recipes/<id>
   # -> 401

   curl -i -X DELETE -H "Authorization: Bearer $DARA"  http://localhost:8080/recipes/<id>
   # -> 403

   curl -i -X DELETE -H "Authorization: Bearer $SOKHA" http://localhost:8080/recipes/<id>
   # -> 204
   ```

3. **Remove the `removeRecipe` endpoint** from `RecipeController`. It was only there to show the
   gap; the client never asked for it.

### Why 403 and not 500?

The denied call throws `AccessDeniedException` inside the service. It travels out of Spring MVC to
Spring Security, which answers 403. Your `GlobalExceptionHandler` lets it pass because it only
handles specific exceptions. **If anyone adds a catch-all `@ExceptionHandler(Exception.class)`, it
catches the denial first and Dara gets 500.** The fix is a more specific handler:

```java
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You are not allowed to do this.");
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/AccessDenied"));
    problemDetail.setTitle("Access denied");
    return problemDetail;
  }
```

(`org.springframework.security.access.AccessDeniedException`). You don't need it today.

### It is an aspect — Session 8

`@PreAuthorize` works because `RecipeServiceImpl` is a **proxy**, like your `ServiceCallAspect`. So
Session 8's rule applies: a call to `delete(...)` from **another method inside `RecipeServiceImpl`**
skips the check. A call from any other bean — today's controller, next month's clean-up job — is
checked.

---

## Where you ended up

| Request | Nobody | Dara (`USER`) | Sokha (`ADMIN`) |
|---|---|---|---|
| `POST /auth/register`, `POST /auth/login` | open | open | open |
| any `GET` on `/recipes/**` or `/tags/**` | 200 | 200 | 200 |
| `POST /recipes`, `PUT /recipes/{id}` | 401 | 201 / 200 | 201 / 200 |
| `PUT /recipes/{id}/tags/{name}` | 401 | 204 | 204 |
| `DELETE /recipes/{id}` | 401 | 403 | 204 |
| `GET /me` | 401 | 200 | 200 |
| anything with a forged or expired token | 401 | 401 | 401 |

`mvn test` still passes as long as PostgreSQL is running: `contextLoads` starts the whole app,
security included.

**This is not finished.** A token lives 15 minutes and then Dara must type her password again; there
is no logout; a browser front-end on another port cannot call the API yet. Next session adds refresh
tokens, logout and "Log in with GitHub" — and shows CORS and proper error bodies.

---

## If you get stuck

**`WeakKeyException: The specified key byte array is ... bits`** at startup — `app.jwt.secret` is
shorter than 32 characters.

**Every `POST` is `403`, even with a token** — `.csrf(csrf -> csrf.disable())` is missing.

**Anonymous requests get `403` instead of `401`** — the `.exceptionHandling(...)` line is missing.

**`required a bean of type 'UserMapper' that could not be found`** — the `componentModel =
MappingConstants.ComponentModel.SPRING` part of `@Mapper` is missing, or the project was not rebuilt
after the mapper was added: MapStruct writes `UserMapperImpl` at compile time.

**MapStruct warns `Unmapped target properties: "id, passwordHash, role, authorities"`** — the four
`@Mapping(... ignore = true)` lines are missing from `toAppUser`.

**`/auth/register` answers `201` but `role` is `null`, or the app fails with `null value in column
"password_hash"`** — `register` in `AuthServiceImpl` does not set the hash or the role. The mapper
leaves both empty on purpose.

**`/auth/login` answers `401` with the right password** — Dara was never registered (checkpoint 3),
or the email has a typo. Check `select email from users;`.

**A valid token still gets `401`** — the filter is not in the chain: `.addFilterBefore(...)` is
missing, or `JwtAuthenticationFilter` has no `@Component`. Or the header is `Bearer` without the
space, or `Bearer` twice.

**`/me` answers `500` with `ClassCastException: class java.lang.String cannot be cast to class
...AppUser`** — the request reached the service with nobody signed in, so the principal was the
text `"anonymousUser"`. `/me` must be covered by `anyRequest().authenticated()`: check that no
`permitAll()` rule above it matches `/me`.

**`/auth/register` is `401`** — the `/auth/**` rule is missing, or sits **below**
`anyRequest()`.

**`Schema validation: missing table [users]`** — `004-create-users.sql` is not in
`db.changelog-master.yaml`.

**Every user gets `403` on `hasRole`, even Sokha** — `getAuthorities()` in `AppUser` returns
`role.name()` without the `ROLE_` prefix. `hasRole("ADMIN")` looks for `ROLE_ADMIN`. Or the prefix is
written twice: `"ROLE_ROLE_ADMIN"`.

**`AppUser is not abstract and does not override abstract method getAuthorities()`** — one of the
three `UserDetails` methods is missing, or misspelt.

**Dara's `DELETE` is `204` with the URL rule** — the `DELETE` line sits **below**
`anyRequest().authenticated()`. First match wins.

**Dara's `DELETE` is `204` after `@PreAuthorize`** — `@EnableMethodSecurity` is missing from
`SecurityConfig`. Without it the annotation is just a comment.

**Sokha's delete is `403`** — her row has the wrong `role` (`select email, role from users;`), or
the token was made before you changed it. Log in again.

**`The dependencies of some of the beans form a cycle`** — `SecurityConfig` must not inject
`AuthService`, and `JwtAuthenticationFilter` must not inject `SecurityConfig`. Keep the arrows as in
this file.
