# Tokens that last, and GitHub login

*Stay signed in, sign out for real, skip the password.*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause**
prompts and actually answer them before reading on.*

**Before you start**

- **You need the Recipe API from last session, finished** — JWT login, your
  `JwtAuthenticationFilter`, `GET /me`, and `@PreAuthorize` on `delete`. Copy it into this session's
  `recipe-api/` folder and continue from there; that folder's `README.md` says how.
- **A GitHub account**, for checkpoint 2.
- **Java 25 · Spring Boot 4.1.1 · Maven · PostgreSQL · JJWT 0.13.0.**
- **Two checkpoints.** `mvn test` is green when you start and green at the end of every
  checkpoint; what you add today you check with `curl` and a browser.

**By the end you'll be able to:**

- Explain why an access token is short-lived, and what a refresh token is for
- Rotate refresh tokens, and say what happens when a used one comes back
- Explain the OAuth2 authorization code flow, and what the `state` and the code are for
- Let people log in with GitHub, and hand them **your** tokens
- Give every 401 and 403 a body that tells the client *why*, and let a browser on another origin
  call the API

---

## Who you are building for today

**Sunrise Kitchen's** mobile and web teams tried the API and came back with:

- *"Fifteen minutes is fine for the token — but don't make people type their password every fifteen
  minutes."*
- *"When someone logs out, or loses their phone, that has to actually end their session."*
- *"Half our cooks already have GitHub accounts. Let them log in with that — no new password to
  remember."*

---

## Where last session left us

Last session ended with a working login: `POST /auth/login` checks the password once and returns a
JWT, and your `JwtAuthenticationFilter` accepts it on every request. Two loose ends, and one new
request:

1. **The token dies after 15 minutes**, and Dara must type her password again.
2. **There is no logout.** A JWT is accepted until its `exp`, whatever happens on the phone.
3. *"Half our cooks already have GitHub accounts. Let them log in with that."*

### A JWT cannot be cancelled

The API accepts a valid JWT by checking its signature. That is the point — no lookup of the token.
But it also means that once a token is out, nothing stops it before `exp`: Dara logs out, her phone
is stolen, the token keeps working. (A **deleted** user is out at once, because your filter loads the
user on every request — but logging out deletes nobody.)

The only control you have is **how long it lives**:

| Access token lifetime | If stolen | For the user |
|---|---|---|
| 7 days | 7 days of access for the thief | logs in once a week |
| 15 minutes | 15 minutes | logs in every 15 minutes |

Neither works. So we split the job across two tokens.

---

## Refresh tokens

| | Access token | Refresh token |
|---|---|---|
| What it is | a signed JWT | 32 random bytes |
| Lives | 15 minutes | 7 days |
| Sent | on **every** request | **only** to `POST /auth/refresh` |
| Checked by | its signature | a lookup in the `refresh_tokens` table |
| Can be cancelled | no | yes — mark it revoked |

The access token stays short, so a stolen one is worth little. The refresh token is long-lived but
travels rarely, and because the server **looks it up**, it can be cancelled. When the access
token expires, the app quietly swaps the refresh token for a new pair. The user types the password
once a week.

### Why the refresh token is not a JWT

A JWT has one advantage: the server checks it with the signature alone, without asking the
database. A refresh token gets nothing from that, because every use already goes to the database:

- **Logout** has to cancel it straight away. A signed token stays valid until it expires.
- **It works once.** "This token was already used" is something we store — in the table.
- **Nothing to leak.** A random value has no signing key behind it, and the table holds only hashes.

Make it a JWT and you still need the table for all three. You pay for both designs and get the
benefit of neither. One way to remember it: the access token is a **signed ID card** checked at the
door; the refresh token is a **ticket number** the server looks up in its own records.

### Stored as a hash

The table stores `sha256(token)`, never the token. A leaked table gives an attacker hashes that
work nowhere.

Why SHA-256, when last session said never to use a fast hash for passwords? Because a password is
something a person **chose**, and people choose guessable things — BCrypt's slowness is a defence
against guessing. A refresh token is 256 **random** bits. There is nothing to guess; the fastest
computer on Earth would not find one by trying. A fast hash is enough.

### Rotation: each refresh token works once

| The app sends | The server revokes | The app gets back |
|---|---|---|
| email and password, to `/auth/login` | — | access `A1`, refresh `R1` |
| `R1`, to `/auth/refresh` | `R1` | access `A2`, refresh `R2` |
| `R2`, to `/auth/refresh` | `R2` | access `A3`, refresh `R3` |

### Reuse detection: the theft alarm

Now suppose a thief copied R2:

| Who | Sends | The server sees |
|---|---|---|
| Dara | `R2` | unused — revokes it, hands her `R3` |
| the thief | `R2` | **revoked — but it came back** |

A revoked token coming back means **two people had it**. We cannot tell which one is Dara, so we
revoke **every** refresh token she has. The thief is out. Dara is out too — and simply logs in
again with her password, which the thief does not have.

```java
if (stored.isRevoked()) {
  refreshTokenRepository.revokeAllFor(stored.getEmail());
  throw new InvalidRefreshTokenException("refresh token already used");
}
```

### A transaction that must *not* roll back

`use` is `@Transactional`. It calls `revokeAllFor` and then **throws**. By default an exception
rolls the transaction back — which would quietly undo the revoke and leave the thief signed in. So:

```java
@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
```

The persistence sessions taught `@Transactional` as "all or nothing". This is the case where *nothing* is the
wrong answer.

### What logout really does

`POST /auth/logout` revokes the refresh token. The access token from that login **still works**
until it expires — at most fifteen minutes. That is the price of a token checked by its signature, and the reason
it is short. If fifteen minutes is too long for your system, the answers are a shorter
lifetime or a deny-list lookup on every request — and the second gives back what JWTs saved.

> **Pause.** Dara logs out at 10:00. Her access token was issued at 09:58. Until when can someone
> holding that access token still call the API? And if Sokha **deleted** Dara's account at 10:00
> instead?

---

### Where the client keeps the tokens

Not our code, but the first question every front-end developer asks you.

| Client | Access token | Refresh token |
|---|---|---|
| Mobile app | memory | the phone's secure storage (Keychain, Keystore) |
| Browser app | JavaScript memory | an `HttpOnly`, `Secure` cookie the page's JavaScript cannot read |

**Not `localStorage`.** Any script on the page can read it — including one injected through a bug
(XSS) — and send it anywhere.

Notice the browser row: **a cookie is back**. And with a cookie, the CSRF attack from last session is
back too. A real browser setup turns CSRF protection on again for the refresh endpoint. That is why
last session called switching it off *a decision, not a shortcut*.

---

## Checkpoint 1

*"Don't make people type their password every fifteen minutes. But when someone logs out, it has to
actually end their session."*

Changeset `005`, `RefreshToken`, `RefreshTokenService`, `refresh` and `logout` in `AuthService`, and
two endpoints. **`recipe-api/README.md`, checkpoint 1.** `mvn test` green.

Refresh once, replay the old token and watch the newer one die too, then log out. Read the
`refresh_tokens` table — hashes, never tokens.

---

## Break — 10 minutes

---

## OAuth2 — let GitHub check the password

### The weakness built into what we built

**Our API stores passwords.** So it must hash them (done), reset them by email, lock accounts after
too many guesses, offer two-factor login — and get every one of those right. And every new site
asks people for one more password to remember.

Most sites now offer *"Log in with Google / GitHub / Microsoft"* instead. The person proves who they
are to a company that already knows them, and that company tells your site. The rules for how that
conversation goes are **OAuth2**.

### The four roles

| Role | In our story |
|---|---|
| **Resource owner** | Dara, the person |
| **Client** | **our Recipe API** — it asks GitHub who Dara is |
| **Authorization server** | GitHub — the only place Dara types her GitHub password |
| **Resource server** | GitHub's API (`/user`) — it answers with Dara's profile |

Notice our API is the **client** here, not the resource server. That is why the starter is
`spring-boot-starter-security-oauth2-client`.

### The flow — authorization code

| Step | From | To | What travels |
|---|---|---|---|
| 1 | browser | our API | `GET /oauth2/authorization/github` |
| 2 | our API | browser | "go to GitHub" — with a random `state` |
| 3 | browser | GitHub | Dara signs in and clicks **Authorize** |
| 4 | GitHub | browser | "go back to `/login/oauth2/code/github?code=…&state=…`" |
| 5 | our API | GitHub | the code + our client secret → a GitHub access token |
| 6 | our API | GitHub | `GET /user` with that token → Dara's profile |
| 7 | our API | browser | "go to the front-end" — with **our** token and refresh token |

Spring Security does 1–6. You write 7, in `GitHubLoginHandler`.

**Why a code, and not the token straight away?** Step 4 travels through the browser, where URLs end
up in history, logs and screenshots. A code is useless on its own: only someone with our **client
secret** can swap it for a token, in step 5 — server to server, never through the browser. And it
works once.

**The `state`.** In step 2 our API makes up a random value, and remembers it. GitHub hands it back in
step 4. If it does not match, someone is trying to push *their* login into *your* browser, and Spring
refuses.

> You may spot `code_challenge=…` in the redirect too. That is an extra lock Spring adds on its own —
> nothing to configure, nothing to write. Ignore it today.

### Where does Spring remember the `state`? — `STATELESS`, revisited

Between step 2 and step 4 the API must remember the `state`. Spring keeps it in an HTTP session,
for about a minute — and your chain says `STATELESS`. Does that break it?

No. `STATELESS` means one thing: **Spring Security never keeps *who is logged in* in a session**.
The GitHub trip keeps its own small note there, and that is all the session ever holds. After step 7,
every request to the API carries our Bearer token, exactly as last session, and no session is
created. Open DevTools during a GitHub login and you see a `JSESSIONID` appear; call `/me` and the
response sets none.

### GitHub gives us a person — we give out our tokens

GitHub's token proves the person to **GitHub**. We do not use it to call our own API: we read the
profile once, then forget it.

```java
AppUser user =
    appUserRepository
        .findByGithubId(profile.id())
        .orElseGet(() -> createGitHubUser(profile));
return tokensFor(user);   // AppUser is a UserDetails since last session
```

- **Found by GitHub's id, not the email.** An email can change; GitHub's id for a person never does.
- **No email?** GitHub hides it if the person made it private. GitHub's own
  `<id>+<login>@users.noreply.github.com` takes its place.
- **Everyone who arrives from GitHub is a `USER`.** A login never makes anyone an admin.
- **`tokensFor` is the same method a password login ends with.** From here on, a GitHub user and a
  password user look identical to the rest of the API — the filter, the rules, `@PreAuthorize`,
  refresh, logout.

A GitHub user has **no password** in our table, so `006` makes `password_hash` nullable, and
`AppUser.getPassword()` returns `null` for them. BCrypt never matches a `null` hash, so
`/auth/login` cannot be used to get into a GitHub account — and no line of the security code
changes.

### The tokens go after the `#`

```
http://localhost:5173/oauth2/callback#token=eyJ…&refreshToken=q3B0…&expiresIn=900
```

The part after `#` — the **fragment** — is never sent to a server. The browser keeps it, and the
front-end's JavaScript reads it. The tokens never land in a web server's access log.

### One change to the chain

```java
.oauth2Login(oauth -> oauth
    .successHandler(gitHubLoginHandler)
    .failureHandler(gitHubLoginHandler))
```

`oauth2Login` adds two filters to the chain: one for `/oauth2/authorization/github`, one for
`/login/oauth2/code/github`. They sit before `AuthorizationFilter`, so those two URLs need no rule.

**And it changes one default.** With `oauth2Login` on, Spring's answer to "not signed in" becomes
*"go and log in with GitHub"* — a 302 to `/oauth2/authorization/github`. For an API that is wrong:
a phone app wants a 401, not a GitHub web page. Last session's line keeps it right:

```java
.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
```

### Our token or a provider's token?

| | Our tokens, our passwords (last session) | Log in with GitHub (today) | A provider issues the API's tokens |
|---|---|---|---|
| Who checks the password | our API, with BCrypt | GitHub | the provider (Keycloak, Entra ID) |
| Whose JWT the API accepts | ours | **ours** | the provider's, checked with its public key |
| Spring setup | `JwtService` + your filter | + `oauth2Login` | `oauth2ResourceServer().jwt()` + `issuer-uri` |
| Fits | one API, one app | one API, people who have GitHub | many APIs, one company login |

The third column is where large systems end up. The filter chain, the URL rules, 401 versus 403 and
`@PreAuthorize` stay the same in all three.

> **Pause.** Someone copies the `/login/oauth2/code/github?code=…&state=…` address out of Dara's
> browser history an hour later and opens it. What happens, and which two things stop them?

---

## Checkpoint 2

*"Half our cooks already have GitHub accounts. Let them log in with that — no new password to
remember."*

A GitHub OAuth App, the OAuth2 client starter, changeset `006`, `loginWithGitHub` in the service,
`GitHubLoginHandler`, and `.oauth2Login(...)`. **`recipe-api/README.md`, checkpoint 2.** `mvn test`
green.

Log in with GitHub in a browser, call `/me` with the token from the address bar — the same
`UserService` as last session answers. Log in again: same row. Then press Cancel, and replay the
callback.

---

## Errors before the controller — shown in class

*Shown in class, built after class — this and CORS, below. The steps are near the end of the
README.* GitHub login sends the browser to a web app on `localhost:5173`; the moment that app exists,
it needs both.

Your `GlobalExceptionHandler` turns exceptions into `ProblemDetail` bodies — for exceptions thrown
**inside Spring MVC**. A 401 or 403 from the security chain happens **before** `DispatcherServlet`,
in a filter. `@RestControllerAdvice` cannot reach it.

| Where the error happens | Who writes the answer |
|---|---|
| filter chain — not signed in | the `AuthenticationEntryPoint` → 401 |
| filter chain — signed in, not allowed | the `AccessDeniedHandler` → 403 |
| `DispatcherServlet` → controller → service | your `@RestControllerAdvice` — everything else |

Last session used `HttpStatusEntryPoint`: the right status, an empty body. A front-end needs more. The
web app should **quietly refresh** on "token expired", and **show the login page** on "no token". So
we write the two handlers ourselves, in the same shape as the rest of the API:

```java
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    Object reason = request.getAttribute(JwtAuthenticationFilter.REJECTED_TOKEN);
    String detail = reason != null ? reason.toString() : "No token. Log in and send it as: Authorization: Bearer <token>";
    ProblemDetailWriter.write(request, response, 401, "Not signed in", detail);
  }
}
```

**How does the entry point know why?** The filter still never answers by itself. It catches the
exception, leaves the reason on the **request** as an attribute, and carries on anonymous. Later in
the chain, `ExceptionTranslationFilter` calls the entry point, which reads the attribute:

```java
} catch (ExpiredJwtException e) {
  SecurityContextHolder.clearContext();
  request.setAttribute(REJECTED_TOKEN, "Token expired. Refresh it at /auth/refresh");
} catch (JwtException | UsernameNotFoundException e) {
  SecurityContextHolder.clearContext();
  request.setAttribute(REJECTED_TOKEN, "Token invalid");
}
```

`ExpiredJwtException` comes first: it is itself a `JwtException`, and Java takes the first `catch`
that matches.

The `AccessDeniedHandler` also catches the denial from `@PreAuthorize` — it travels out of Spring MVC
to `ExceptionTranslationFilter`, which hands it over. So Dara's denied delete gets a body as well.

---

## CORS — letting a browser call you

An **origin** is scheme + host + port. `http://localhost:5173` and `http://localhost:8080` are
**different origins**. By default, a browser lets a page call only its own origin; this is the
**same-origin policy**, and it is what stops `evil.com` from reading your bank's API with your
cookies.

**CORS** (Cross-Origin Resource Sharing) is how a server says *"this other origin may call me"*.
For anything but the simplest request, the browser first sends a **preflight**:

```http
OPTIONS /recipes
Origin: http://localhost:5173
Access-Control-Request-Method: POST
Access-Control-Request-Headers: authorization, content-type
```

and only sends the real `POST` if the answer allows it:

```http
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: authorization, content-type
```

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
  CorsConfiguration config = new CorsConfiguration();
  config.setAllowedOrigins(allowedOrigins);
  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
  config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
  config.setMaxAge(3600L);
  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/**", config);
  return source;
}
```

and `.cors(Customizer.withDefaults())` in the chain, so Spring Security's `CorsFilter` answers the
preflight **before** the authorisation rules — a preflight carries no token, and must not be refused
for that.

Three things people get wrong:

- **Only browsers enforce CORS.** `curl`, Postman and other servers ignore it entirely. CORS
  protects *users' browsers*, not your API. It is not access control — your URL rules and tokens are.
- **Never allow `*` together with credentials.** A wildcard origin plus an `Authorization` header or
  cookie would let any website act as your user. List the real front-ends.
- **The allowed origins are configuration.** `app.cors.allowed-origins` differs between your laptop
  and the server, so it is a property, not code.

> **Pause.** The web team says "CORS is blocking us" and suggests `allowedOrigins("*")`. What do
> you answer, and what do you ask them for instead?

---

## A recap of the token half

- A JWT cannot be cancelled → **15 minutes**. A refresh token lives longer, is stored **hashed**,
  works **once**, and a reused one **revokes everything**. `noRollbackFor` keeps that revoke.
- Logout revokes the refresh token; the access token runs out on its own.
- **OAuth2 authorization code:** the browser goes to GitHub and comes back with a one-time **code**;
  the API swaps it, with its **secret**, server to server. The **`state`** stops forged callbacks.
- `STATELESS` still holds: the GitHub trip keeps only its `state` in a session for a minute.
- GitHub tells us **who**; **we** hand out the tokens. A GitHub user looks like any other user from
  then on.
- Errors from the chain need an **entry point** and an **access-denied handler**. **CORS** answers
  the browser's preflight for named origins — never `*`.

---

## What you have *not* learned

| Missing | Why it matters |
|---|---|
| **Refresh tokens per device** | Our theft alarm logs Dara out everywhere. Real systems keep a token *family* per device and end only that one. |
| **Refresh token in an `HttpOnly` cookie** | The safest browser setup — and it brings CSRF protection back for that endpoint. |
| **A provider issuing the API's tokens** | Keycloak or Entra ID signing with RS256, the API checking with `oauth2ResourceServer().jwt()` — the third column of *Our token or a provider's token?* |
| **Linking accounts** | Dara registered with a password *and* has GitHub: today those are two accounts, or an `email_already_registered`. Linking them safely needs a verified email. |
| **Key rotation** | One HS256 secret forever is a risk. Providers rotate keys and publish them. |
| **Rate limiting on login** | Nothing stops a thousand password guesses a minute at `/auth/login`. |
| **HTTPS** | Tokens are as sensitive as passwords in transit. Next session's host puts HTTPS in front of the API for you. |

---

## What you now know that you did not

| Before today | Now |
|---|---|
| A token died after 15 minutes, and so did the session | A **refresh token** — random, hashed, single-use — keeps the user signed in for a week |
| Nothing could end a session | Logout revokes the refresh token; a **reused** one revokes every session — the theft alarm |
| `@Transactional` meant all or nothing | `noRollbackFor` — the case where *nothing* is the wrong answer |
| Every user needed a password with us | **Log in with GitHub**: GitHub checks the password, a one-time code comes back, the API swaps it and hands out **its own** tokens |
| `STATELESS` sounded like "no session, ever" | It means *who is logged in* is never in a session — the GitHub trip keeps only its `state` |
| A 401 from the chain had an empty body | An **entry point** and an **access-denied handler** write ProblemDetails that say why |
| A browser on another port could not call the API | **CORS** answers the preflight for named origins — never `*` |

**Next:** shipping it — containers, a pipeline, and a public URL your client can open.

---

## Extra learning material — testing token rules

> Optional. Not done in class.

With `spring-boot-starter-security-test` (last session's extra), a test can call your real endpoints
and check the bodies:

```java
@SpringBootTest
@AutoConfigureMockMvc
class TokenRulesTest {

  @Autowired MockMvc mockMvc;

  @Test
  void noTokenSaysWhy() throws Exception {
    mockMvc.perform(post("/recipes"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(startsWith("No token")));
  }

  @Test
  void preflightFromTheWebAppIsAllowed() throws Exception {
    mockMvc.perform(options("/recipes")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }
}
```

A refresh-token test is a good next step: log in, refresh, replay the old token, and expect the
newer one to be dead too.
