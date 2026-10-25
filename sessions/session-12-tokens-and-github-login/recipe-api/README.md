# Exercise — The Recipe API: tokens that last, and GitHub login

Same project as last session. Session 11 ended with a working login, and two loose ends: the token
dies after 15 minutes and Dara must type her password again, and there is no real logout. The first
checkpoint ties those off — and the second lets people skip the password entirely, by logging in
with their **GitHub** account.

**Java 25 · Spring Boot 4.1.1 · Maven · PostgreSQL · JJWT 0.13.0.**

**You need PostgreSQL running for both checkpoints, and a GitHub account for checkpoint 2.**

---

## Who you are building for today

**Sunrise Kitchen's** mobile and web teams tried the API and came back with:

- *"Fifteen minutes is fine for the token — but don't make people type their password every fifteen
  minutes."*
- *"When someone logs out, or loses their phone, that has to actually end their session."*
- *"Half our cooks already have GitHub accounts. Let them log in with that — no new password to
  remember."*

## Start here

**This folder has no code in it — only this file.** You bring the code: today continues the project
you finished last session.

1. Copy your Session 11 project into this folder — everything in
   `../../session-11-how-a-request-gets-authorised/recipe-api/` **except** its `README.md`. From
   this folder:

   ```bash
   cp -r ../../session-11-how-a-request-gets-authorised/recipe-api/{pom.xml,src,mvnw,mvnw.cmd,.mvn} .
   ```

   On Windows, copy `pom.xml`, `src`, `mvnw`, `mvnw.cmd` and `.mvn` across in File Explorer.
2. In the copied `pom.xml`, change `<artifactId>recipe-api-security</artifactId>` to
   `<artifactId>recipe-api-tokens</artifactId>`.
3. **Open the `pom.xml`, not the folder**, when you import it into IntelliJ.
4. Run `mvn test` and start the application once. Check Session 11 still works:

   ```bash
   curl -s -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"cook1234"}' http://localhost:8080/auth/login
   # -> {"token":"eyJ...","tokenType":"Bearer","expiresIn":900,"email":"dara@sunrisekitchen.com"}
   ```

**Today builds on all of Session 11.** Before you start, your project should have:

| From last session | Used today in |
|---|---|
| `SecurityConfig` — one chain, `STATELESS`, `JwtAuthenticationFilter` in it | Checkpoint 2 adds GitHub login to the same chain |
| `AuthService` / `AuthServiceImpl` with `register(user, password)` and `login(email, password)`, `AuthController` | Checkpoint 1 adds refresh and logout; checkpoint 2 adds GitHub |
| `users` table (changeset `004`), `AppUser`, `CustomUserDetailsService`, `UserMapper` | Checkpoint 1 links refresh tokens to it; checkpoint 2 changes it |
| `GET /me` — `UserController` → `UserService.findByEmail` | Checkpoint 2 uses it to check a GitHub login |
| Dara registered (`cook1234`), Sokha seeded (`chef1234`) | Every check |
| From Session 10: `RecipeService` / `RecipeServiceImpl`, `TagService` / `TagServiceImpl`, `TagController` | Unchanged today — every service in the project is an interface and an `Impl` |

### What you will add today

```
com.sunrise.recipeapi
├── domain/        + RefreshToken                              (checkpoint 1)
│                    AppUser                                   (gains githubId in checkpoint 2)
├── repository/    + RefreshTokenRepository                    (checkpoint 1)
│                    AppUserRepository                         (+ findByGithubId, checkpoint 2)
├── mapper/          UserMapper                                (one line, checkpoint 2)
├── service/       + RefreshTokenService, InvalidRefreshTokenException      (checkpoint 1)
│                    AuthService, AuthServiceImpl              (grow in checkpoints 1 and 2)
├── controller/      AuthController                            (grows in checkpoint 1)
├── dto/           + RefreshRequest;  AuthResponse grows        (checkpoint 1)
│                  + GitHubProfile                             (checkpoint 2)
├── security/      + GitHubLoginHandler                        (checkpoint 2)
│                    SecurityConfig, CustomUserDetailsService  (one change each, checkpoint 2)
└── resources/db/changelog/   + 005-create-refresh-tokens.sql  (checkpoint 1)
                              + 006-github-login.sql           (checkpoint 2)
```

---

## What you check today

**`mvn test` stays green at the end of every checkpoint**, with PostgreSQL running — and without
any GitHub settings: the `not-set` defaults let the app start.

| Checkpoint | What you do | How you check it |
|---|---|---|
| 1 | Refresh tokens — stored hashed, used once — and logout | `curl` a refresh, a replay, a logout · the `refresh_tokens` table |
| 2 | Log in with GitHub, hand out **our** tokens | a browser login · `/me` with the token · one GitHub user row · two deliberate breaks |

One part is **not** a checkpoint: the error bodies and CORS are shown in class and built after
class. It is written out in this file.

### Pace

Checkpoint 2 is the slowest: it needs a GitHub OAuth App, which takes five minutes on GitHub before
any code.

---

## Checkpoint 1 — Stay signed in, and really sign out

**What the client asked for:** *"Don't make people type their password every fifteen minutes. But
when someone logs out, or loses their phone, that has to actually end their session."*

A JWT cannot be cancelled: the API accepts it until `exp`. So it stays short, and a second token does
the long-lived job:

| | Access token (Session 11) | Refresh token (today) |
|---|---|---|
| Looks like | a signed JWT | 43 random characters |
| Lives | 15 minutes | 7 days |
| Sent | on every request | only to `POST /auth/refresh` |
| The server checks it by | its signature | looking it up in the database |
| Can be cancelled? | no — it just expires | yes — mark it revoked |

### The changeset

Create `src/main/resources/db/changelog/005-create-refresh-tokens.sql`:

```sql
--liquibase formatted sql

--changeset sunrise:005-create-refresh-tokens
create table refresh_tokens (
    id         bigint generated by default as identity primary key,
    token_hash varchar(64)              not null unique,
    email      varchar(100)             not null references users (email),
    expires_at timestamp with time zone not null,
    revoked    boolean                  not null default false
);
--rollback drop table refresh_tokens;
```

and add it at the bottom of `db.changelog-master.yaml`:

```yaml
  - include:
      file: 'db/changelog/005-create-refresh-tokens.sql'
```

`token_hash` holds a **SHA-256 of the token**, never the token. A leaked table gives hashes that
work nowhere.

> Why SHA-256 here, when Session 11 said "never SHA-256 for passwords"? A password is something a
> person **chose** — guessable, which is what BCrypt's slowness defends against. A refresh token is
> 32 **random** bytes. There is nothing to guess, so a fast hash is enough.

### The entity and the repository

`domain/RefreshToken.java`:

```java
package com.sunrise.recipeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // SHA-256 of the token, never the token itself
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  public RefreshToken(String tokenHash, String email, Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.email = email;
    this.expiresAt = expiresAt;
  }
}
```

`repository/RefreshTokenRepository.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query("update RefreshToken t set t.revoked = true where t.email = :email and t.revoked = false")
  int revokeAllFor(@Param("email") String email);
}
```

`revokeAllFor` is a bulk update — Session 10's `@Modifying`. You see why it exists when you replay a token below.

### `RefreshTokenService`

Add to `application.properties`:

```properties
# 7 days
app.jwt.refresh-expiration-ms=604800000
```

`service/InvalidRefreshTokenException.java`:

```java
package com.sunrise.recipeapi.service;

public class InvalidRefreshTokenException extends RuntimeException {
    InvalidRefreshTokenException(String message) {
        super(message);
    }
}
```

`service/RefreshTokenService.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.RefreshToken;
import com.sunrise.recipeapi.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${app.jwt.refresh-expiration-ms}")
  private long refreshExpirationMs;

  // 32 random bytes. Only its hash is stored; the token itself goes to the client once.
  @Transactional
  public String issue(String email) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    Instant expiresAt = Instant.now().plus(Duration.ofMillis(refreshExpirationMs));
    refreshTokenRepository.save(new RefreshToken(hash(token), email, expiresAt));
    return token;
  }

  // Uses the token up: it is revoked, and the caller gets the email back
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public String use(String token) {
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(hash(token))
            .orElseThrow(() -> new InvalidRefreshTokenException("unknown refresh token"));

    if (stored.isRevoked()) {
      // A used token came back: someone has a copy. End every session of this user.
      refreshTokenRepository.revokeAllFor(stored.getEmail());
      throw new InvalidRefreshTokenException("refresh token already used");
    }
    if (stored.getExpiresAt().isBefore(Instant.now())) {
      throw new InvalidRefreshTokenException("refresh token expired");
    }

    stored.setRevoked(true);
    return stored.getEmail();
  }

  @Transactional
  public void revoke(String token) {
    refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(t -> t.setRevoked(true));
  }

  private static String hash(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
```

Three rules live in `use`:

- **Each refresh token works once.** Using it revokes it, and the caller gets a new one —
  **rotation**.
- **A revoked token coming back means theft.** The real user and a thief both had a copy; whoever
  came second holds a dead one. We cannot tell which is which, so **every** session of that user
  ends, and the real user simply logs in again.
- **`noRollbackFor`.** `use` throws *after* `revokeAllFor`. By default an exception rolls the
  transaction back — and the "revoke everything" would be undone. Session 10's `@Transactional`,
  with a reason to change its default.

### The response, the request, the service

Replace `dto/AuthResponse.java` — it gains `refreshToken`:

```java
package com.sunrise.recipeapi.dto;

public record AuthResponse(
    String token, String refreshToken, String tokenType, long expiresIn, String email) {}
```

Create `dto/RefreshRequest.java`:

```java
package com.sunrise.recipeapi.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

Replace `service/AuthService.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.AuthResponse;

public interface AuthService {
    /**
     * Create an account with the USER role
     * Throws EmailAlreadyExistsException if the email is taken
     * @param user the new user, without a password hash yet
     * @param password the password as the user typed it; only its hash is stored
     * @return the saved user
     */
    AppUser register(AppUser user, String password);

    /**
     * Check the password once, and hand out an access token and a refresh token
     * Wrong email or password throws BadCredentialsException
     * @param email the email the user logs in with
     * @param password the password as the user typed it
     * @return both tokens
     */
    AuthResponse login(String email, String password);

    /**
     * Swap an unused refresh token for a new pair. The old one is used up
     * @param refreshToken the refresh token from the last login or refresh
     * @return a new access token and a new refresh token
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Revoke the refresh token. The access token still works until it expires
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);
}
```

Replace `service/AuthServiceImpl.java`:

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
import org.springframework.security.core.userdetails.UserDetailsService;
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
  private final UserDetailsService userDetailsService;
  private final RefreshTokenService refreshTokenService;

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
    return tokensFor((UserDetails) authentication.getPrincipal());
  }

  @Override
  public AuthResponse refresh(String refreshToken) {
    String email = refreshTokenService.use(refreshToken);
    // Load the user again: a changed role or a removed user shows up here
    return tokensFor(userDetailsService.loadUserByUsername(email));
  }

  @Override
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  private AuthResponse tokensFor(UserDetails user) {
    return new AuthResponse(
        jwtService.generateToken(user),
        refreshTokenService.issue(user.getUsername()),
        "Bearer",
        jwtService.expirationSeconds(),
        user.getUsername());
  }
}
```

`register` is Session 11's, unchanged. `login` and `refresh` now end the same way, in `tokensFor`: a new access token **and** a new refresh
token. On refresh the user is **loaded again** — if Sokha changed Dara's role, Dara's next token
shows it.

### Two endpoints, one handler

Replace `controller/AuthController.java`:

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.dto.LoginRequest;
import com.sunrise.recipeapi.dto.RefreshRequest;
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

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request.email(), request.password()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Validated @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(authService.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Validated @RequestBody RefreshRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
```

Add to `GlobalExceptionHandler`:

```java
import com.sunrise.recipeapi.service.InvalidRefreshTokenException;
```

```java
  @ExceptionHandler(InvalidRefreshTokenException.class)
  ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getLocalizedMessage());
    problemDetail.setType(URI.create("recipe-api.sunrise.com/docs/InvalidRefreshToken"));
    problemDetail.setTitle("Invalid refresh token");
    return problemDetail;
  }
```

`/auth/refresh` and `/auth/logout` are already open: `/auth/**` is `permitAll()` since Session 11.
They need no access token — the refresh token *is* the proof.

### Your turn

1. `mvn test` — **green.**
2. Restart. Liquibase runs `005`. Log in as Dara and keep **both** tokens:

   ```bash
   curl -s -H "Content-Type: application/json" \
     -d '{"email":"dara@sunrisekitchen.com","password":"cook1234"}' http://localhost:8080/auth/login
   # -> {"token":"eyJ...","refreshToken":"q3B0...","tokenType":"Bearer","expiresIn":900,"email":"dara@..."}

   REFRESH=q3B0...
   ```

3. **Refresh — a new pair, and the old refresh token is used up:**

   ```bash
   curl -i -H "Content-Type: application/json" -d "{\"refreshToken\":\"$REFRESH\"}" \
     http://localhost:8080/auth/refresh
   # -> 200 with a NEW token and a NEW refreshToken. Keep the new one:
   NEW_REFRESH=...
   ```

4. **Replay the old one — the theft alarm:**

   ```bash
   curl -i -H "Content-Type: application/json" -d "{\"refreshToken\":\"$REFRESH\"}" \
     http://localhost:8080/auth/refresh
   # -> 401 "refresh token already used"

   curl -i -H "Content-Type: application/json" -d "{\"refreshToken\":\"$NEW_REFRESH\"}" \
     http://localhost:8080/auth/refresh
   # -> 401 "refresh token already used"   <- the new one died too: every session ended
   ```

5. **Log out** — log in again, keep the refresh token as `R`, then:

   ```bash
   curl -i -H "Content-Type: application/json" -d "{\"refreshToken\":\"$R\"}" \
     http://localhost:8080/auth/logout
   # -> 204

   curl -i -H "Content-Type: application/json" -d "{\"refreshToken\":\"$R\"}" \
     http://localhost:8080/auth/refresh
   # -> 401
   ```

6. Look at the table:

   ```bash
   psql -d recipes_db -c "select email, revoked, expires_at, left(token_hash, 12) from refresh_tokens;"
   ```

   Hashes, never tokens. Every used or logged-out row says `revoked = t`.

> **What logout does not do:** the *access* token from that login still works until it expires — up
> to fifteen minutes. That is the price of a token the API checks without the database, and the
> reason it is short.

---

## Break — 10 minutes

---

## Checkpoint 2 — Log in with GitHub

**What the client asked for:** *"Half our cooks already have GitHub accounts. Let them log in with
that — no new password to remember."*

The person types their password on **GitHub**, never on our API. GitHub tells us who they are. We
find or create their account — and from there it is Session 11 again: **our** JWT, **our** refresh
token, our rules.

| Step | Who | What happens |
|---|---|---|
| 1 | the browser opens `/oauth2/authorization/github` | our API redirects it to GitHub, with a random `state` |
| 2 | Dara, on GitHub | signs in and clicks **Authorize** |
| 3 | GitHub | sends the browser back to `/login/oauth2/code/github?code=…&state=…` — a one-time code |
| 4 | our API | swaps the code for a GitHub token, then asks GitHub *"who is this?"* |
| 5 | our API | sends the browser on to the front-end, with **our** token and refresh token |

Spring Security does steps 1, 3 and 4 for you. You write step 5.

### Register the API with GitHub

On GitHub: **Settings → Developer settings → OAuth Apps → New OAuth App**
(<https://github.com/settings/developers>).

| Field | Value |
|---|---|
| Application name | `Recipe API (your name)` |
| Homepage URL | `http://localhost:8080` |
| Authorization callback URL | `http://localhost:8080/login/oauth2/code/github` |

**Register application**, then **Generate a new client secret**. Copy the **Client ID** and the
**client secret** now — GitHub shows the secret only once.

The callback URL must match **exactly**. It is how GitHub knows it may send the browser back to you.

### The starter

In `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-client</artifactId>
</dependency>
```

*Client*: our API is a client of GitHub — it asks GitHub who the user is.

### The settings

Add to `application.properties`:

```properties
# From your GitHub OAuth App. Never commit the secret: it comes from the environment.
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID:not-set}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET:not-set}

# Where the browser goes after a GitHub login, carrying our tokens
app.oauth2.redirect-uri=http://localhost:5173/oauth2/callback
```

Spring Boot already knows GitHub's addresses — the login page, the token URL, the `/user` URL. The
name `github` in the property is enough.

Now give IntelliJ the two values: **Run → Edit Configurations… → your application → Environment
variables**:

```
GITHUB_CLIENT_ID=Ov23li...;GITHUB_CLIENT_SECRET=1a2b3c...
```

The `:not-set` defaults are there so the app — and `mvn test` — still start without them. GitHub
login just fails until the real values are set.

### The changeset — a user without a password

Create `src/main/resources/db/changelog/006-github-login.sql`:

```sql
--liquibase formatted sql

--changeset sunrise:006-github-login
-- People who log in with GitHub have no password here
alter table users alter column password_hash drop not null;
-- GitHub's id for the person, so the next login finds the same account
alter table users add column github_id bigint unique;
--rollback alter table users drop column github_id;
--rollback alter table users alter column password_hash set not null;
```

and add it at the bottom of `db.changelog-master.yaml`:

```yaml
  - include:
      file: 'db/changelog/006-github-login.sql'
```

### `AppUser`, its repository, and the mapper

In `domain/AppUser.java`, **replace** the `passwordHash` field and add `githubId` under it:

```java
  // BCrypt hash, never the password itself. null for people who log in with GitHub
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  // GitHub's id for this person -- set only if they logged in with GitHub
  @Column(name = "github_id", unique = true)
  private Long githubId;
```

`nullable = false` is gone from `passwordHash`: the column allows `null` since `006`, and
`ddl-auto=validate` checks that the entity agrees.

In `repository/AppUserRepository.java`, add:

```java
  Optional<AppUser> findByGithubId(Long githubId);
```

In `mapper/UserMapper.java`, `toAppUser` gets **one more** ignore line — a registration never sets a
GitHub id:

```java
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "githubId", ignore = true)
    AppUser toAppUser(RegisterRequest request);
```

**`CustomUserDetailsService` does not change.** It returns the `AppUser` itself, and for a GitHub
user `getPassword()` returns `null`. BCrypt never matches a `null` hash — any password sent to
`/auth/login` for that email is a `401` — so a GitHub user cannot sneak in through the password
door.

### Find or create the user

`dto/GitHubProfile.java`:

```java
package com.sunrise.recipeapi.dto;

// What GitHub told us about the person who just logged in
public record GitHubProfile(Long id, String login, String name, String email) {}
```

In `service/AuthService.java`, add:

```java
    /**
     * GitHub said who this is. Find or create their account, and hand out OUR tokens
     * Throws EmailAlreadyExistsException if a password account already has that email
     * @param profile what GitHub told us about the person
     * @return both tokens, as for a password login
     */
    AuthResponse loginWithGitHub(GitHubProfile profile);
```

In `service/AuthServiceImpl.java`, add the two methods above `tokensFor`:

```java
  @Override
  @Transactional
  public AuthResponse loginWithGitHub(GitHubProfile profile) {
    // Seen this GitHub account before? Use its user. First time? Create one.
    AppUser user =
        appUserRepository
            .findByGithubId(profile.id())
            .orElseGet(() -> createGitHubUser(profile));
    // AppUser is a UserDetails -- it goes straight into tokensFor
    return tokensFor(user);
  }

  private AppUser createGitHubUser(GitHubProfile profile) {
    // GitHub sends no email if the person keeps it private
    String email =
        profile.email() != null
            ? profile.email()
            : profile.id() + "+" + profile.login() + "@users.noreply.github.com";
    if (appUserRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException("email: " + email + " already registered");
    }
    AppUser user = new AppUser();
    user.setEmail(email);
    user.setFullName(profile.name() != null ? profile.name() : profile.login());
    user.setRole(Role.USER);
    user.setGithubId(profile.id());
    // No password: this person proves who they are through GitHub
    return appUserRepository.save(user);
  }
```

New import, in both files: `com.sunrise.recipeapi.dto.GitHubProfile`.

- **The GitHub id, not the email, finds the account.** An email can change; GitHub's id for a
  person never does.
- **No email?** GitHub hides it if the person made it private. GitHub's own
  `<id>+<login>@users.noreply.github.com` address takes its place.
- **Every GitHub user is a `USER`.** Nobody becomes an admin by logging in.
- **`tokensFor` is checkpoint 1's**: an access token **and** a refresh token, exactly as for a
  password login.

### What happens when GitHub sends the browser back

`security/GitHubLoginHandler.java`:

```java
package com.sunrise.recipeapi.security;

import com.sunrise.recipeapi.dto.AuthResponse;
import com.sunrise.recipeapi.dto.GitHubProfile;
import com.sunrise.recipeapi.service.AuthService;
import com.sunrise.recipeapi.service.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Runs when GitHub sends the browser back to us
@Component
@RequiredArgsConstructor
public class GitHubLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
  private final AuthService authService;

  // Where the front-end waits for the tokens
  @Value("${app.oauth2.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    // Spring Security has already put the GitHub login in the SecurityContext
    OAuth2User github =
        (OAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    GitHubProfile profile =
        new GitHubProfile(
            ((Number) github.getAttribute("id")).longValue(),
            github.getAttribute("login"),
            github.getAttribute("name"),
            github.getAttribute("email"));

    try {
      AuthResponse tokens = authService.loginWithGitHub(profile);
      // After the #, so the tokens stay in the browser and never reach a server log
      response.sendRedirect(
          redirectUri
              + "#token=" + tokens.token()
              + "&refreshToken=" + tokens.refreshToken()
              + "&expiresIn=" + tokens.expiresIn());
    } catch (EmailAlreadyExistsException e) {
      response.sendRedirect(redirectUri + "#error=email_already_registered");
    }
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    // The user pressed "Cancel" on GitHub, or the login could not be checked
    response.sendRedirect(redirectUri + "#error=github_login_failed");
  }
}
```

Spring Security has already done the hard part when `onAuthenticationSuccess` runs: it checked the
`state`, swapped the code for a GitHub token, and fetched the profile. You turn that profile into
**our** tokens.

The GitHub user comes out of the `SecurityContext`, the same way `getCurrentUser()` read the
`AppUser` in Session 11. Spring Security puts the finished login there **before** it calls
`onAuthenticationSuccess`, so it is already waiting. This time the principal is an `OAuth2User`:
GitHub's profile as a map of attributes, not our `AppUser`.

The tokens go **after the `#`** in the URL. That part — the *fragment* — stays in the browser: it is
never sent to the front-end's server, so it never lands in a server log.

### Switch it on

In `security/SecurityConfig.java`, the `securityFilterChain` method gets one more parameter:

```java
  SecurityFilterChain securityFilterChain(HttpSecurity http, GitHubLoginHandler gitHubLoginHandler)
      throws Exception {
```

and the chain one more block, just above `.authenticationProvider(...)`:

```java
        // "Log in with GitHub": /oauth2/authorization/github starts it,
        // GitHub sends the browser back to /login/oauth2/code/github
        .oauth2Login(oauth -> oauth
            .successHandler(gitHubLoginHandler)
            .failureHandler(gitHubLoginHandler))
```

**That is the whole change to the chain.** It stays `STATELESS`; the rules, the JWT filter and the
entry point are Session 11's.

> **Why a parameter, not a field?** `GitHubLoginHandler` needs `AuthService`, which needs the
> `AuthenticationManager`, which `SecurityConfig` creates. As a constructor field that is a circle,
> and Spring refuses to start. As a parameter of the `@Bean` method it is asked for later, when
> `SecurityConfig` already exists.

### Your turn

1. `mvn test` — **green**, without any GitHub settings.
2. Restart. Liquibase runs `006`.
3. **Log in with GitHub.** In a browser, open:

   ```
   http://localhost:8080/oauth2/authorization/github
   ```

   GitHub's page asks *"Authorize Recipe API (your name)?"* — click **Authorize**. The browser lands
   on `http://localhost:5173/oauth2/callback#token=eyJ...&refreshToken=...`, and the page says
   *"This site can't be reached"* — **that is fine**: there is no front-end on 5173 yet. Everything
   you need is in the address bar.
4. Copy the `token` from the address bar and use it:

   ```bash
   curl -i -H "Authorization: Bearer eyJ..." http://localhost:8080/me
   # -> 200 {"id":3,"email":"583231+yourlogin@users.noreply.github.com","fullName":"Your Name","role":"USER"}
   #    (or your real email, if it is public on GitHub)
   ```

   It is **our** token — signed by `JwtService`, read by your filter, answered by Session 11's
   `UserService`. GitHub's token never left the server.
5. Look at the table:

   ```bash
   psql -d recipes_db -c "select email, role, github_id, password_hash from users;"
   ```

   Your GitHub row: a `github_id`, `USER`, and **no** `password_hash`.
6. **Log in with GitHub again.** Same row — no new user. The `github_id` found you.
7. **The refresh token works too:**

   ```bash
   curl -i -H "Content-Type: application/json" -d '{"refreshToken":"<from the address bar>"}' \
     http://localhost:8080/auth/refresh
   # -> 200, a new pair — checkpoint 1, unchanged
   ```

8. **Try `/auth/login` as the GitHub user** with any password: `401`. There is no password to match.

### Break it on purpose

**Press Cancel.** Start the login again and click **Cancel** on GitHub's page:

```
http://localhost:5173/oauth2/callback#error=github_login_failed
```

**Replay the callback.** After a successful login, press the browser's **Back** button until the
`/login/oauth2/code/github?code=...` address comes back, and load it again:

```
#error=github_login_failed
```

The `state` was used up on the first trip, and the code is single-use too. Someone who copies that
URL out of your history gets nothing.

**Look for the session.** Open the browser's DevTools → **Application → Cookies → localhost:8080**.
A `JSESSIONID` appears **during** the GitHub trip: Spring keeps the `state` there for a minute, to
check it when GitHub sends the browser back. Now call `/me` with `curl -i` and read the response
headers: **no** `Set-Cookie`. The API is still stateless — `STATELESS` means Spring never keeps
*who is logged in* in a session. The GitHub trip keeps only its own small note.

---

## Errors a front-end can read, and CORS — shown in class, built after class

*Not a checkpoint. Shown in class; the steps are here to build afterwards.* GitHub login above
redirects to the web app on `localhost:5173` — and the moment that web app exists, it will need both
of these.

**What the client asked for:** *"The browser refuses to call you from `localhost:5173`. And when you
say 401, tell us why."*

Two problems, both in front of your controllers — where `@RestControllerAdvice` cannot reach.

**Empty 401s.** Session 11's `HttpStatusEntryPoint` answers with a status and no body. The web app
cannot tell "you never logged in" from "your token expired — refresh it". The second one should make
it call `/auth/refresh` quietly; the first should show the login page.

**CORS.** A page loaded from `http://localhost:5173` that calls `http://localhost:8080` is talking to
a **different origin**. The browser first sends a *preflight* `OPTIONS` request asking *"may this
origin call you, with these headers?"*. Today the API has no answer, so the browser refuses — before
your code is ever reached. `curl` does not care about CORS; **only browsers enforce it**.

### One writer for error bodies

`security/ProblemDetailWriter.java`:

```java
package com.sunrise.recipeapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Writes the same ProblemDetail shape as GlobalExceptionHandler -- but from a filter,
// where @RestControllerAdvice cannot reach.
final class ProblemDetailWriter {
  private ProblemDetailWriter() {}

  static void write(
      HttpServletRequest request, HttpServletResponse response, int status, String title, String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\"}"
                .formatted(title, status, detail, request.getRequestURI()));
  }
}
```

The same four fields as the `ProblemDetail`s from your `GlobalExceptionHandler`, so the front-end
reads every error the same way. (The `detail` texts are fixed strings, so writing the JSON by hand is
safe here.)

### 401 and 403 with a body

`security/JsonAuthenticationEntryPoint.java` — *not signed in*:

```java
package com.sunrise.recipeapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Not signed in -> 401, with a body that says why
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    // Set by JwtAuthenticationFilter when a token was sent but rejected
    Object reason = request.getAttribute(JwtAuthenticationFilter.REJECTED_TOKEN);
    String detail = reason != null ? reason.toString() : "No token. Log in and send it as: Authorization: Bearer <token>";
    ProblemDetailWriter.write(request, response, 401, "Not signed in", detail);
  }
}
```

`security/JsonAccessDeniedHandler.java` — *signed in, but not allowed*:

```java
package com.sunrise.recipeapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Signed in, but not allowed -> 403, same shape
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    ProblemDetailWriter.write(
        request, response, 403, "Access denied", "You are signed in, but not allowed to do this.");
  }
}
```

The access-denied handler also catches the `AccessDeniedException` from `@PreAuthorize`, so Dara's
denied delete gets a body too.

### The filter says why it rejected a token

Replace `security/JwtAuthenticationFilter.java`:

```java
package com.sunrise.recipeapi.security;

import io.jsonwebtoken.ExpiredJwtException;
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
  // Read by JsonAuthenticationEntryPoint to explain a 401
  static final String REJECTED_TOKEN = "jwt.rejected";

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
    } catch (ExpiredJwtException e) {
      SecurityContextHolder.clearContext();
      request.setAttribute(REJECTED_TOKEN, "Token expired. Refresh it at /auth/refresh");
    } catch (JwtException | UsernameNotFoundException e) {
      // Forged, malformed, or for a user who no longer exists: stay anonymous
      SecurityContextHolder.clearContext();
      request.setAttribute(REJECTED_TOKEN, "Token invalid");
    }

    filterChain.doFilter(request, response);
  }
}
```

Only the `catch` changed: an **expired** token and an **invalid** one are now told apart, and the
reason is left on the request for the entry point to read. The filter still never answers by itself.

### CORS, and the new handlers, in the config

Add to `application.properties`:

```properties
# The front-ends allowed to call the API from a browser. Comma-separated.
app.cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

Replace `security/SecurityConfig.java`:

```java
package com.sunrise.recipeapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final UserDetailsService userDetailsService;
  private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
  private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

  @Value("${app.cors.allowed-origins}")
  private List<String> allowedOrigins;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, GitHubLoginHandler gitHubLoginHandler)
      throws Exception {
    http
        // A JSON API: no login form, no session cookie, so no CSRF token
        .csrf(csrf -> csrf.disable())
        // Let the browser front-end call us -- uses the corsConfigurationSource bean below
        .cors(Customizer.withDefaults())
        // Nothing is remembered between requests: the token is the session
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // The rules, read from top to bottom, first match wins
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
            .requestMatchers("/auth/**", "/error").permitAll()
            .anyRequest().authenticated())
        // 401 and 403 with a ProblemDetail body, like the rest of the API
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(jsonAuthenticationEntryPoint)
            .accessDeniedHandler(jsonAccessDeniedHandler))
        // "Log in with GitHub" -- checkpoint 2
        .oauth2Login(oauth -> oauth
            .successHandler(gitHubLoginHandler)
            .failureHandler(gitHubLoginHandler))
        .authenticationProvider(authenticationProvider())
        // Read the Bearer token before Spring's own login filter would run
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // How long the browser may remember this answer, in seconds
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
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

New: `.cors(Customizer.withDefaults())` and the `corsConfigurationSource` bean it uses, and the two
JSON handlers in `.exceptionHandling(...)` instead of `HttpStatusEntryPoint`. The GitHub
`.oauth2Login(...)` block from checkpoint 2 stays exactly where it was.

| CORS setting | Means |
|---|---|
| `allowedOrigins` | which pages may call us — **never `*` with `Authorization` headers** |
| `allowedMethods` | the HTTP methods they may use, including the `OPTIONS` preflight |
| `allowedHeaders` | `Authorization` for the token, `Content-Type` for JSON |
| `maxAge` | how long the browser may cache the answer before asking again |

### Try it

**Errors that say why:**

```bash
curl -i -X POST http://localhost:8080/recipes
# -> 401 application/problem+json
# {"title":"Not signed in","status":401,"detail":"No token. Log in and send it as: Authorization: Bearer <token>","instance":"/recipes"}

curl -i -X POST -H "Authorization: Bearer abc.def.ghi" http://localhost:8080/recipes
# -> 401 {"title":"Not signed in","status":401,"detail":"Token invalid","instance":"/recipes"}
```

Set `app.jwt.expiration-ms=30000`, restart, log in, wait 30 seconds, and send the token:

```
401 {"title":"Not signed in","status":401,"detail":"Token expired. Refresh it at /auth/refresh","instance":"/me"}
```

**Set it back to `900000`.** Then Dara's delete:

```bash
curl -i -X DELETE -H "Authorization: Bearer $DARA" http://localhost:8080/recipes/1
# -> 403 {"title":"Access denied","status":403,"detail":"You are signed in, but not allowed to do this.","instance":"/recipes/1"}
```

**CORS — play the browser's preflight with `curl`:**

```bash
curl -i -X OPTIONS http://localhost:8080/recipes \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type"
# -> 200
# Access-Control-Allow-Origin: http://localhost:5173
# Access-Control-Allow-Headers: authorization, content-type

curl -i -X OPTIONS http://localhost:8080/recipes \
  -H "Origin: http://evil.example" \
  -H "Access-Control-Request-Method: POST"
# -> 403 Invalid CORS request
```

Every 401 and 403 now has a ProblemDetail body that says why; the preflight from `localhost:5173` is
allowed, and an unknown origin is refused.

---

## Where you ended up

| Request | Nobody | Dara (password) | A GitHub user | Sokha (`ADMIN`) |
|---|---|---|---|---|
| `POST /auth/login` | — | access + refresh token | 401 — no password | access + refresh token |
| `/oauth2/authorization/github` | → GitHub → our tokens | — | → GitHub → our tokens | — |
| `POST /auth/refresh` with an unused refresh token | — | new pair | new pair | new pair |
| `POST /auth/refresh` with a used one | — | 401, every session ends | 401, every session ends | 401, every session ends |
| `POST /auth/logout` | — | 204 | 204 | 204 |
| any `GET` on `/recipes/**` or `/tags/**` | 200 | 200 | 200 | 200 |
| `POST` / `PUT` with a valid access token | 401 | 201 / 200 | 201 / 200 | 201 / 200 |
| `DELETE /recipes/{id}` | 401 | 403 | 403 | 204 |

`mvn test` still passes as long as PostgreSQL is running. It does not need the GitHub settings: the `not-set` defaults let the app start.

---

## If you get stuck

**`/auth/refresh` says `refresh token already used` on the first try** — the token was pasted with a
quote or a space at the end, or it was already used by an earlier `curl`. Log in again.

**MapStruct warns `Unmapped target property: "githubId"`** — the `githubId` ignore line is missing
from `UserMapper.toAppUser` (checkpoint 2).

**`Schema validation: missing table [refresh_tokens]`** or **`missing column [github_id]`** — `005` or
`006` is not in `db.changelog-master.yaml`.

**GitHub says `The redirect_uri is not associated with this application`** — the callback URL in
your OAuth App is not exactly `http://localhost:8080/login/oauth2/code/github`.

**GitHub says the application was not found, or `client_id` is invalid** — `GITHUB_CLIENT_ID` is not
set in the run configuration, so the app is sending `not-set`.

**The login lands on `#error=github_login_failed` every time** — the client secret is wrong or
expired; generate a new one. Or the browser is blocking cookies for `localhost`: the GitHub trip
needs its `JSESSIONID`.

**`#error=email_already_registered`** — your public GitHub email is the same as an account that
registered with a password. Log in with the password, or make your GitHub email private.

**`The dependencies of some of the beans form a cycle`** — `GitHubLoginHandler` is a constructor
field of `SecurityConfig`. Make it a parameter of `securityFilterChain` (checkpoint 2, *Switch it on*).


**Every `/me` request opens a GitHub page** — the `.exceptionHandling(...)` line with the 401 entry
point was removed. Without it, `oauth2Login` makes "log in with GitHub" the answer to every
anonymous request.
