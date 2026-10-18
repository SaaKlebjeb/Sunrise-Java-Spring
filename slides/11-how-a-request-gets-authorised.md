---
layout: cover
---

# How a request gets authorised

Session 11 · Java Enterprise Spring Framework

<div class="opacity-70 mt-4">The Recipe API gets accounts, a login, and a signed token</div>

---

# By the end of today you can

- tell **authentication** from **authorisation** — and **401** from **403**
- follow a request through the **filter chain** to your controller
- write a `SecurityFilterChain` that opens some URLs and protects the rest
- store users with **BCrypt** hashes, and let people **register**
- issue a signed **JWT** at login — and accept it with a **filter you wrote**
- protect a service method with `@PreAuthorize`

---

# The brief for today

<div class="p-5 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-lg mt-4">

*"Before we put this online, we need to know who's using it."*

*"Anyone can browse recipes. To add or change one, you need an account."*

*"People sign up themselves. Our head chef, Sokha, is the only admin."*

*"Log in once on the phone — don't make the app keep the password."*

*"Deleting a recipe is final. Only the head chef can do that."*

</div>

---
layout: section
---

# Why "just check the user in the controller" fails

---

# The first idea everyone has

```java {all|3|4-6}
@DeleteMapping("/recipes/{id}")
public ResponseEntity<Void> deleteRecipe(@PathVariable Long id,
                                         @RequestHeader("X-User") String user) {
  if (!user.equals("sokha")) {
    return ResponseEntity.status(403).build();
  }
  recipeService.delete(id);
  return ResponseEntity.noContent().build();
}
```

---

# Four problems

<div class="grid grid-cols-2 gap-4 mt-4">
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

**A claim, not a proof**

`curl -H "X-User: sokha"` works for everyone.

</div>
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

**Remember it everywhere**

Forget it on one endpoint and that endpoint is open.

</div>
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Wrong layer**

"Only the head chef deletes" is a business rule — a **service** rule since Session 8.

</div>
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Too late**

The body was already parsed and validated.

</div>
</div>

---

# What we want instead

<div class="grid grid-cols-3 gap-4 mt-6">
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Proof**

A password checked once — then a signed token.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**One gate**

Every request passes it. The default answer is **no**.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Readable rules**

URL rules in one file, plus rules on the actions.

</div>
</div>

---
layout: section
---

# Two words, two ways

---

# Authentication vs authorisation

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-5 rounded-lg border-2 border-blue-500 bg-blue-500/10">

### Authentication

***Who are you?***

A password, a token, a certificate.

Fails with **401 Unauthorized**

</div>
<div class="p-5 rounded-lg border-2 border-purple-500 bg-purple-500/10">

### Authorisation

***Are you allowed to do this?***

Rules about URLs and roles.

Fails with **403 Forbidden**

</div>
</div>

<div class="opacity-70 text-sm mt-4">"401 Unauthorized" is a historical misname. It means <b>unauthenticated</b>.</div>

---

# After login — how does the server remember you?

HTTP forgets everything between requests.

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Session** — stateful

Server stores a session, browser keeps a cookie.

Every request: cookie → server looks it up.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Token** — stateless

Server signs a token, client keeps it.

Every request: token → server **checks the signature**.

</div>
</div>

---

# Logbook or library card

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Session = library logbook**

The librarian writes your name in at each visit. Fine for one branch. Chaos across fifty.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Token = stamped library card**

Any branch checks the stamp without calling headquarters.

</div>
</div>

| | Session | Token |
|---|---|---|
| Server stores | every session | nothing |
| Three servers | must share sessions | any server checks any token |
| Fits | server-rendered pages | phones, JavaScript apps |

---

# HTTP Basic — it exists

```
Authorization: Basic ZGFyYTpjb29rMTIzNA==        ← dara:cook1234, Base64
```

- The username and password, on **every** request
- Spring's default — you'll see it for one minute in checkpoint 1
- Fine for a script on `localhost`

<div class="mt-4 p-3 rounded-lg border-2 border-red-500 bg-red-500/10">
Wrong for an app: the phone must <b>store</b> the password, and the server runs BCrypt on <b>every</b> request. We skip it and go straight to tokens.
</div>

---
layout: section
---

# Checkpoint 1

Lock the door

---

# The brief

<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-xl mt-4">
"Before we put this online, we need to know who's using it."
</div>

<div class="mt-6">

Copy your Session 10 project, rename it **`recipe-api-security`**.

Add **one** dependency. Nothing else.

</div>

---

# One dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Restart:

```
Using generated security password: 3f1c9a2e-....-....

This generated password is for development use only. Your security
configuration must be updated before running your application in production.
```

---

# Try it

```bash
curl -i http://localhost:8080/recipes
# 401   WWW-Authenticate: Basic realm="Realm"

curl -i -u user:PASTE-PASSWORD http://localhost:8080/recipes
# 200   ← that -u is HTTP Basic
```

You wrote **zero** lines. Spring Boot locked every endpoint, made a user called `user`, and switched on Basic.

---

# Secure by default

<div class="grid grid-cols-2 gap-6 mt-6">
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

**Open by default**

Every endpoint you forget is **public**. Fails quietly.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Locked by default**

Every endpoint you forget is **closed**. Fails loudly. Someone fixes it.

</div>
</div>

<div class="mt-6 text-center">Auto-configuration again — like Tomcat in Session 5. Now we open what should be open.</div>

---
layout: section
---

# The filter chain

What a request passes through

---

# The trip one request takes

<div class="flex flex-col items-center gap-1 text-sm mt-2">

<div class="border-2 border-gray-500 rounded-lg px-6 py-1 bg-gray-500/10 font-mono">curl</div>
<div class="text-xl opacity-60 leading-none">↓</div>
<div class="border-2 border-gray-500 rounded-lg px-6 py-1 bg-gray-500/10"><b>Tomcat</b></div>
<div class="text-xl opacity-60 leading-none">↓</div>

<div class="flex items-stretch gap-2">
<div class="border-2 border-gray-400 rounded-lg px-4 py-2 opacity-70 flex items-center">filter</div>
<div class="border-2 border-red-500 rounded-lg px-5 py-2 bg-red-500/10 text-center">
<div class="font-bold text-base">Spring Security</div>
<div class="text-xs opacity-80">can answer 401 or 403 itself — and stop</div>
</div>
<div class="border-2 border-gray-400 rounded-lg px-4 py-2 opacity-70 flex items-center">filter</div>
</div>
<div class="text-xl opacity-60 leading-none">↓</div>

<div class="flex items-stretch gap-2">
<div class="border-2 border-purple-500 rounded-lg px-4 py-2 bg-purple-500/10"><b>DispatcherServlet</b></div>
<div class="flex items-center text-2xl opacity-60">→</div>
<div class="border-2 border-green-600 rounded-lg px-4 py-2 bg-green-500/10"><b>controller</b></div>
<div class="flex items-center text-2xl opacity-60">→</div>
<div class="border-2 border-blue-500 rounded-lg px-4 py-2 bg-blue-500/10"><b>service</b></div>
<div class="flex items-center text-2xl opacity-60">→</div>
<div class="border-2 border-amber-500 rounded-lg px-4 py-2 bg-amber-500/10"><b>repository</b></div>
</div>

</div>

<div class="mt-4">If security says no, <b>your controller never runs</b>.</div>

---

# One filter, holding a chain

<div class="mt-2 p-3 rounded-lg border-2 border-gray-500 bg-gray-500/10 text-sm">
<div class="font-mono font-bold">DelegatingFilterProxy <span class="font-sans font-normal text-xs opacity-70">— the one filter Tomcat knows about</span></div>
<div class="mt-2 p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10">
<div class="font-mono font-bold">FilterChainProxy <span class="font-sans font-normal text-xs opacity-70">— a Spring bean</span></div>
<div class="mt-2 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
<div class="font-mono font-bold">SecurityFilterChain <span class="font-sans font-normal text-xs opacity-70">— runs top to bottom</span></div>
<div class="mt-2 flex flex-col gap-1">
<div class="border-2 border-gray-400 opacity-60 rounded-lg px-3 py-1 font-mono">CsrfFilter<span class="text-xs opacity-80 ml-2">we switch it off</span></div>
<div class="border-2 border-amber-500 bg-amber-500/10 rounded-lg px-3 py-1 font-mono">JwtAuthenticationFilter<span class="text-xs opacity-80 ml-2">yours, from checkpoint 5</span></div>
<div class="border-2 border-gray-400 rounded-lg px-3 py-1 font-mono">AnonymousAuthenticationFilter</div>
<div class="border-2 border-gray-400 rounded-lg px-3 py-1 font-mono">ExceptionTranslationFilter</div>
<div class="border-2 border-gray-400 rounded-lg px-3 py-1 font-mono">AuthorizationFilter</div>
</div>
</div>
</div>
</div>

<div class="opacity-70 text-sm mt-2">Simplified. The real chain has about fifteen. You need five.</div>

---

# The five that matter today

| Filter | Its job |
|---|---|
| `CsrfFilter` | rejects browser writes without a CSRF token |
| **`JwtAuthenticationFilter`** | reads `Authorization: Bearer …`, checks the token |
| `AnonymousAuthenticationFilter` | nobody signed in? mark the request "anonymous" |
| `ExceptionTranslationFilter` | "not signed in" → **401**, "not allowed" → **403** |
| `AuthorizationFilter` | the last gate: checks your URL rules |

---

# The `SecurityContext`

When an authentication filter is satisfied, it stores the user here:

<div class="mt-4 p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">
<div class="font-mono font-bold">SecurityContext</div>
<div class="mt-2 p-4 rounded-lg border-2 border-green-600 bg-green-500/10">
<div class="font-mono font-bold">Authentication</div>

| | |
|---|---|
| `principal` | the user — a `UserDetails` |
| `authorities` | `[ ROLE_USER ]` |
| `authenticated` | `true` |

</div>
</div>

Every later filter, `@PreAuthorize`, and your controller read the user from here.

<div class="opacity-70 text-sm">Checkpoint 5's <code>GET /me</code> takes the email out of this object.</div>

---

# No token at all, step by step

<div class="flex flex-col gap-1 mt-2 text-sm">
<div class="border-2 border-gray-500 bg-gray-500/10 rounded-lg px-4 py-2"><span class="font-mono font-bold">POST /recipes</span> <span class="opacity-70 ml-2">no <code>Authorization</code> header</span></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div>
<div class="grid grid-cols-[18rem_1fr] items-center border-2 border-amber-500 bg-amber-500/10 rounded-lg px-4 py-2"><span class="font-mono">JwtAuthenticationFilter</span><span>no <code>Bearer </code> — nothing to do</span></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div>
<div class="grid grid-cols-[18rem_1fr] items-center border-2 border-gray-400 rounded-lg px-4 py-2"><span class="font-mono">AnonymousAuthenticationFilter</span><span>marks the request <b>anonymous</b></span></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div>
<div class="grid grid-cols-[18rem_1fr] items-center border-2 border-red-500 bg-red-500/10 rounded-lg px-4 py-2"><span class="font-mono">AuthorizationFilter</span><span>rule says <code>authenticated</code> — <b>denied</b></span></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div>
<div class="grid grid-cols-[18rem_1fr] items-center border-2 border-red-500 bg-red-500/10 rounded-lg px-4 py-2"><span class="font-mono">ExceptionTranslationFilter</span><span>anonymous? → entry point → <b>401</b></span></div>
<div class="mt-2 border-2 border-dashed border-gray-400 rounded-lg px-4 py-2 opacity-60 text-center"><span class="font-mono">DispatcherServlet</span> — never called</div>
</div>

---

# Pause

A request arrives with a **valid** token for Dara, a `USER`, at a URL that needs `ROLE_ADMIN`.

1. Does your JWT filter succeed?
2. Which filter says no?
3. What status code?

<v-click>

<div class="mt-4 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
1. Yes · 2. <code>AuthorizationFilter</code> · 3. <b>403</b> — via <code>ExceptionTranslationFilter</code>
</div>

</v-click>

---
layout: section
---

# Your own rules

`SecurityFilterChain`

---

# Replace the default with a bean

```java {all|4-6|7-10|11-12}
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(
          session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
          .requestMatchers("/auth/**", "/error").permitAll()
          .anyRequest().authenticated())
      .exceptionHandling(
          ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
  return http.build();
}
```

---

# The rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
    .requestMatchers("/auth/**", "/error").permitAll()
    .anyRequest().authenticated())
```

<div class="mt-4 p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-lg">
Top to bottom. <b>First match wins.</b> <code>anyRequest()</code> last — the safety net.
</div>

| Method | Lets through |
|---|---|
| `permitAll()` | everyone |
| `authenticated()` | any signed-in user |
| `hasRole("ADMIN")` | signed in **and** `ROLE_ADMIN` |

---

# Matching paths

| Pattern | Matches | Not |
|---|---|---|
| `/recipes` | `/recipes` | `/recipes/5` |
| `/recipes/*` | `/recipes/5` | `/recipes/5/tags` |
| `/recipes/**` | `/recipes`, `/recipes/5`, `/recipes/5/tags`, `/recipes/filter` | `/tags` |

<div class="opacity-70 text-sm mt-2"><code>/auth/**</code> must be open — you can't need a token to get your first token.</div>

---

# CSRF — why it's off

**Cross-Site Request Forgery** attacks **browsers with a session cookie**:

1. You're logged in to `bank.com` — the browser holds its cookie
2. `evil.com` has a hidden form that posts to `bank.com/transfer`
3. The browser attaches the bank's cookie **by itself**

<div class="mt-4 grid grid-cols-2 gap-4">
<div class="p-3 rounded-lg border-2 border-green-600 bg-green-500/10">

Our API: **no session, no cookie.** The client attaches its token explicitly. The attack doesn't apply.

</div>
<div class="p-3 rounded-lg border-2 border-red-500 bg-red-500/10">

A server-rendered site with a login cookie? **Keep CSRF on.** A decision, not a shortcut.

</div>
</div>

---

# `STATELESS`

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

| Policy | Means |
|---|---|
| `ALWAYS` | always create a session |
| `IF_REQUIRED` | the default |
| **`STATELESS`** | **never** — every request proves itself. **Ours** |

---

# The entry point — the missing 401

```java
.exceptionHandling(
    ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
```

<div class="grid grid-cols-2 gap-4 mt-4">
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

**Without it**

No login method configured → Spring doesn't know how to ask → flat **403**.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**With it**

"Not signed in" → **401**. The right word.

</div>
</div>

<div class="opacity-70 text-sm mt-2">The <b>entry point</b> is what Spring calls when someone needs to authenticate.</div>

---

# 401 vs 403

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-5 rounded-lg border-2 border-amber-500 bg-amber-500/10">

### 401

***"I don't know who you are."***

no token · forged token · expired token · wrong password

Client should: **log in**

</div>
<div class="p-5 rounded-lg border-2 border-red-500 bg-red-500/10">

### 403

***"I know who you are. No."***

signed in, but not an admin

Client should: **give up**

</div>
</div>

---

# Pause — 401 or 403?

1. `POST /recipes` with no token
2. `DELETE /recipes/5` as Dara, a `USER`
3. `GET /me` with an expired token
4. `POST /recipes` with a token whose payload was edited on jwt.io

<v-click>

<div class="mt-4 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
1. <b>401</b> · 2. <b>403</b> · 3. <b>401</b> · 4. <b>401</b> — a broken token means "I don't know who you are"
</div>

</v-click>

---
layout: section
---

# Checkpoint 2

Your own rules

---

# The brief

<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-xl mt-4">
"Anyone can browse recipes. To add or change one, you need an account."
</div>

<div class="mt-6">

Create `security/SecurityConfig.java` — the bean you just saw.

There is **no `.httpBasic()`**. Basic is gone.

</div>

---

# Check

```bash
curl -i http://localhost:8080/recipes
# 200   open again

curl -i -H "Content-Type: application/json" -d '{…}' http://localhost:8080/recipes
# 401   writes need a signed-in user — and there's no way to sign in yet
```

---

# Break it on purpose — the missing 401

1. Comment out `.exceptionHandling(...)`
2. Restart, send the same `POST`

<v-click>

```
403 Forbidden
```

Nobody is signed in — so it must be **401**. **Put the line back.**

</v-click>

---

# Checkpoint 2

- `GET /recipes` → **200**
- anonymous `POST /recipes` → **401**
- (without the entry point → 403 — the wrong word)

---
layout: section
---

# Users

---

# Spring Security asks one question

```java
public interface UserDetailsService {
  UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

***"Give me everything you know about this user."***

- Spring calls it "username". Ours is the **email**.
- The answer: name, **stored hash**, authorities
- Spring compares the passwords itself — **you never do**

---

# `CustomUserDetailsService`

```java
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

<div class="mt-4 opacity-80">One lookup. The row you load <b>is</b> the <code>UserDetails</code> — next: how.</div>

---

# Two ways to give Spring a user

| | Entity `implements UserDetails` — **ours** | Map entity → Spring's `User` |
|---|---|---|
| Code | three methods on `AppUser` | `User.withUsername(...).roles(...).build()` |
| In the `SecurityContext` | **your `AppUser`** — id, name, all of it | a name, a hash, authorities |
| The signed-in user later | one cast, no query | look it up again by email |
| Cost | the entity depends on Spring Security | a copy of the user in another class |

<div class="opacity-70 text-sm mt-2">We pay the first cost on purpose — checkpoint 5's <code>/me</code> is one cast.</div>

---

# The user, and its role

```java
public enum Role {
  USER,
  ADMIN
}
```

```java
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class AppUser implements UserDetails {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true, length = 100)
  private String email;
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;
  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.USER;
  // … and the three UserDetails methods — next slide
}
```

---

# `AppUser` is the `UserDetails`

```java {all|1-4|6-9|11-14}
@Override
public String getUsername() {
  return email;                   // Spring's "username" is our email
}

@Override
public String getPassword() {
  return passwordHash;            // the stored hash -- Spring runs BCrypt
}

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
  return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

<v-click>

<div class="mt-3 p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-sm">
Only <b>three</b> methods have no default in Spring Security 7. <code>isEnabled</code>, <code>isAccountNonLocked</code> and the other two say <code>true</code> — override one the day the client asks for locked accounts.
</div>

</v-click>

---

# Small names, big reasons

<div class="grid grid-cols-3 gap-4 mt-4">
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**`AppUser`, not `User`**

Spring Security already has a class called `User` — one wrong import away.

</div>
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**Table `users`, not `user`**

`user` is a reserved word in PostgreSQL.

</div>
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**`EnumType.STRING`**

Stores `"ADMIN"`, not `1`. Insert a role mid-enum and numbers would shift.

</div>
</div>

---

# Never store the password

```
 email                    | password
--------------------------+----------
 dara@sunrisekitchen.com  | cook1234
```

Who reads this table? A backup on a laptop. A junior with read access. **One** SQL injection.

<div class="mt-4 p-3 rounded-lg border-2 border-red-500 bg-red-500/10">
People reuse passwords. You just leaked their email account too.
</div>

---

# Not SHA-256 either

<div class="p-5 rounded-lg border-2 border-red-500 bg-red-500/10 text-lg mt-4">
A graphics card tries <b>billions</b> of SHA-256 guesses per second. <code>cook1234</code> falls in seconds.
</div>

---

# BCrypt — slow on purpose

<div class="mt-4 flex justify-center font-mono text-lg">
<span class="px-2 py-1 rounded-l-lg border-2 border-gray-500 bg-gray-500/10">$2a$</span><span class="px-2 py-1 border-2 border-l-0 border-amber-500 bg-amber-500/10">10$</span><span class="px-2 py-1 border-2 border-l-0 border-blue-500 bg-blue-500/10">AXn5vDlOtZbox9gVlg9Cqe</span><span class="px-2 py-1 rounded-r-lg border-2 border-l-0 border-green-600 bg-green-500/10">uBjZi2XHLi8vLBmkMaqv1Zz5M97LF1y</span>
</div>

<div class="grid grid-cols-4 gap-2 mt-2 text-sm text-center">
<div class="p-2 rounded-lg border-2 border-gray-500"><b>version</b></div>
<div class="p-2 rounded-lg border-2 border-amber-500"><b>cost</b> 10 = 2¹⁰ rounds</div>
<div class="p-2 rounded-lg border-2 border-blue-500"><b>salt</b> — random, per user</div>
<div class="p-2 rounded-lg border-2 border-green-600"><b>hash</b></div>
</div>

| Defence | What it does |
|---|---|
| **Slow** | fine for one login, painful for billions of guesses |
| **Salted** | same password → different hashes |
| **Tunable** | raise the cost as computers get faster |

```java
@Bean
PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder();
}
```

---

# Roles, and the `ROLE_` prefix

| You write | Spring stores or checks |
|---|---|
| `"ROLE_" + role.name()` in `getAuthorities()` | stores `ROLE_ADMIN` |
| `hasRole("ADMIN")` | checks `ROLE_ADMIN` — **adds** the prefix |
| `hasAuthority("ROLE_ADMIN")` | checks exactly what you wrote |

<div class="mt-4 p-3 rounded-lg border-2 border-amber-500 bg-amber-500/10">

The prefix is written **once** — in `getAuthorities()`. Forget it there and every `hasRole` is **403**, even for Sokha.

</div>

---

# Registering — the same three layers as a recipe

<div class="grid grid-cols-3 gap-4 mt-6">
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**`AuthController`**

HTTP: the body in, `201` out

</div>
<div class="p-4 rounded-lg border-2 border-green-500 bg-green-500/10">

**`AuthService`**

the rules: email free, password **hashed**, role **always `USER`**

</div>
<div class="p-4 rounded-lg border-2 border-purple-500 bg-purple-500/10">

**`UserMapper`**

`RegisterRequest` → `AppUser`<br>`AppUser` → `UserResponse`

</div>
</div>

<div class="mt-6 text-center opacity-80">

`AppUser` in, `AppUser` out — exactly like `RecipeService`. The controller does the mapping.

</div>

---

# The mapper

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    // role is an enum; MapStruct writes its name() into the String
    UserResponse toUserResponse(AppUser user);

    // The service hashes the password and sets the role -- the request decides neither
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    AppUser toAppUser(RegisterRequest request);
}
```

<div class="mt-4 p-3 rounded-lg border-2 border-amber-500 bg-amber-500/10">

`passwordHash` is left empty **on purpose** — a typed password is not a hash.

</div>

---

# The service

```java
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
```

---

# The controller

```java
@PostMapping("/register")
public ResponseEntity<UserResponse> register(@Validated @RequestBody RegisterRequest request) {
  AppUser savedUser = authService.register(userMapper.toAppUser(request), request.password());
  return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toUserResponse(savedUser));
}
```

<div class="mt-6 opacity-80">

Mapper → service → mapper. The same three steps as `createRecipe`.

</div>

---

# Nobody registers as admin

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**Everyone who registers**

gets `Role.USER`. Always.

</div>
<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10">

**The first admin**

comes from changeset `004` — like Fish Amok came from `002`.

</div>
</div>

```sql
--changeset sunrise:004-seed-admin
insert into users (email, password_hash, full_name, role)
values ('sokha@sunrisekitchen.com', '$2a$10$AXn5vDlO…', 'Sokha Chan', 'ADMIN');
```

---
layout: section
---

# Checkpoint 3

Users, and signing up

---

# The brief

<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-xl mt-4">
"People sign up themselves. Our head chef, Sokha, is the only admin."
</div>

| In the README | What |
|---|---|
| The changeset | `004-create-users.sql` — table + Sokha |
| The entity, the repository | `Role`, `AppUser`, `AppUserRepository` |
| Where Spring Security finds a user | `CustomUserDetailsService` |
| A password encoder | the `PasswordEncoder` bean |
| The DTOs and the mapper | `RegisterRequest`, `UserResponse`, `UserMapper` |
| The service, the controller | `AuthService`, `AuthServiceImpl`, `AuthController` |
| A taken email is a 409 | one more handler |

---

# Check

```bash
curl -i -H "Content-Type: application/json" \
  -d '{"email":"dara@sunrisekitchen.com","password":"cook1234","fullName":"Dara Sok"}' \
  http://localhost:8080/auth/register
# 201 {"id":2,"email":"dara@sunrisekitchen.com","fullName":"Dara Sok","role":"USER"}

# again → 409 "email: dara@sunrisekitchen.com already registered"
# bad email, short password → 400  — Session 7's validation, for free
```

```bash
psql -d recipes_db -c "select email, password_hash, role from users;"
```

Register someone else with **the same** password — a **different** hash. The salt.

---

# Checkpoint 3

- Dara registers → **201**, again → **409**
- the `users` table holds hashes
- the generated password has **gone** from the log — your `UserDetailsService` replaced Boot's user

---
layout: center
class: text-center
---

# Break — 10 minutes

<div class="opacity-70 mt-4">After the break: the token</div>

---
layout: section
---

# JWT

JSON Web Token — said "jot"

---

# A festival wristband

<div class="p-5 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-lg mt-4">

It proves you came in legitimately · says what ticket you have · any staff member can check it without calling the box office · and it expires at the end of the day.

</div>

<div class="grid grid-cols-2 gap-4 mt-6">
<div>

- **Self-contained** — carries the user's info
- **Signed** — tamper-proof

</div>
<div>

- **Compact** — fits in a header
- **Stateless** — the server stores nothing

</div>
</div>

---

# Three parts

<div class="mt-4 flex items-stretch justify-center gap-1 font-mono text-sm text-center">
<div class="px-3 py-2 rounded-lg border-2 border-red-500 bg-red-500/10">eyJhbGciOiJIUzI1NiJ9<div class="font-sans font-bold mt-1">header</div></div>
<div class="flex items-center text-2xl font-bold">.</div>
<div class="px-3 py-2 rounded-lg border-2 border-purple-500 bg-purple-500/10">eyJpc3MiOiJyZWNpcGUtYXBpIiwic3ViIjoic29raGFA…<div class="font-sans font-bold mt-1">payload</div></div>
<div class="flex items-center text-2xl font-bold">.</div>
<div class="px-3 py-2 rounded-lg border-2 border-blue-500 bg-blue-500/10">3kq_L0bQ…<div class="font-sans font-bold mt-1">signature</div></div>
</div>

```json
{"alg":"HS256"}
```

```json
{"iss":"recipe-api","sub":"sokha@sunrisekitchen.com","role":"ROLE_ADMIN","iat":1792257025,"exp":1792257925}
```

<div class="opacity-70 text-sm">The real output of today's <code>JwtService</code>.</div>

---

# The claims

| Claim | Means |
|---|---|
| `iss` — issuer | who made it |
| `sub` — subject | who it's about — the email |
| `role` | our own claim |
| `iat` — issued at | seconds since 1970 |
| `exp` — expires | when it stops working |

---

# Readable ≠ forgeable

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-5 rounded-lg border-2 border-amber-500 bg-amber-500/10">

### Anyone can **read** it

Base64 is encoding, not encryption. Paste it into jwt.io.

**Never put a secret in a JWT.**

</div>
<div class="p-5 rounded-lg border-2 border-green-600 bg-green-500/10">

### Nobody can **change** it

The signature needs a **secret** only the server knows.

</div>
</div>

---

# How the signature works

<div class="grid grid-cols-[9rem_1fr] gap-y-3 items-center mt-4 text-sm">

<div class="font-bold">At login — sign</div>
<div class="flex items-stretch gap-2">
<div class="px-3 py-2 rounded-lg border-2 border-purple-500 bg-purple-500/10 font-mono">header.payload</div><div class="flex items-center text-xl opacity-60">+</div><div class="px-3 py-2 rounded-lg border-2 border-amber-500 bg-amber-500/10 font-mono">SECRET</div><div class="flex items-center text-2xl opacity-60">→</div><div class="px-3 py-2 rounded-lg border-2 border-gray-500 bg-gray-500/10 font-mono">HMAC-SHA256</div><div class="flex items-center text-2xl opacity-60">→</div><div class="px-3 py-2 rounded-lg border-2 border-blue-500 bg-blue-500/10 font-mono">signature</div>
</div>

<div class="font-bold">Every request — check</div>
<div class="flex items-stretch gap-2">
<div class="px-3 py-2 rounded-lg border-2 border-purple-500 bg-purple-500/10 font-mono">header.payload</div><div class="flex items-center text-xl opacity-60">+</div><div class="px-3 py-2 rounded-lg border-2 border-amber-500 bg-amber-500/10 font-mono">SECRET</div><div class="flex items-center text-2xl opacity-60">→</div><div class="px-3 py-2 rounded-lg border-2 border-gray-500 bg-gray-500/10 font-mono">HMAC-SHA256</div><div class="flex items-center text-2xl opacity-60">→</div><div class="px-3 py-2 rounded-lg border-2 border-green-600 bg-green-500/10">same as the one sent?</div>
</div>

</div>

<div class="mt-4 p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10">
A <b>wax seal</b>: you can read the letter, but you can't reseal it. Change <code>ROLE_USER</code> to <code>ROLE_ADMIN</code> and the seal breaks.
</div>

---

# The flow

<div class="grid grid-cols-[7rem_1fr_16rem] gap-x-3 gap-y-3 items-center mt-6 text-sm">
<div class="row-span-4 self-stretch flex items-center justify-center rounded-lg border-2 border-gray-500 bg-gray-500/10 font-bold">Phone app</div>
<div class="font-mono text-xs text-center border-b-2 border-amber-500 pb-1">POST /auth/login &#123;email, password&#125; <span class="text-lg">→</span></div>
<div class="p-2 rounded-lg border-2 border-amber-500 bg-amber-500/10">checks the password <b>once</b> — BCrypt</div>
<div class="font-mono text-xs text-center border-b-2 border-amber-500 pb-1">&#123;token: "eyJ…"&#125; <span class="text-lg">←</span></div>
<div class="p-2 rounded-lg border-2 border-amber-500 bg-amber-500/10">signs a token</div>
<div class="font-mono text-xs text-center border-b-2 border-green-600 pb-1">GET /me · Authorization: Bearer eyJ… <span class="text-lg">→</span></div>
<div class="p-2 rounded-lg border-2 border-green-600 bg-green-500/10">checks the <b>signature</b> — no password, no BCrypt</div>
<div class="font-mono text-xs text-center border-b-2 border-green-600 pb-1">200 <span class="text-lg">←</span></div>
<div class="p-2 rounded-lg border-2 border-green-600 bg-green-500/10">answers</div>
</div>

<div class="mt-4 text-sm opacity-70">Server column, top to bottom. Amber happens once. Green happens on every request after.</div>

---

# Pause

Dara opens jwt.io, changes her `exp` to next year, and sends the token.

<v-click>

<div class="mt-4 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
<b>401.</b> Changing <code>exp</code> changed the payload — the signature no longer matches. She'd need the server's secret.
</div>

</v-click>

---

# JJWT

```xml
<jjwt.version>0.13.0</jjwt.version>
```

```xml
<dependency> <groupId>io.jsonwebtoken</groupId> <artifactId>jjwt-api</artifactId>
             <version>${jjwt.version}</version> </dependency>
<dependency> <groupId>io.jsonwebtoken</groupId> <artifactId>jjwt-impl</artifactId>
             <version>${jjwt.version}</version> <scope>runtime</scope> </dependency>
<dependency> <groupId>io.jsonwebtoken</groupId> <artifactId>jjwt-jackson</artifactId>
             <version>${jjwt.version}</version> <scope>runtime</scope> </dependency>
```

<div class="opacity-70 text-sm">You code against <code>api</code>. <code>impl</code> signs and checks; <code>jackson</code> writes the JSON.</div>

---

# The secret comes from outside

```properties
app.jwt.secret=${APP_JWT_SECRET:dev-only-secret-change-me-0123456789abcdef}
app.jwt.expiration-ms=900000
```

- `${APP_JWT_SECRET:…}` — the environment variable if set, otherwise the default
- The default is for **your laptop only** — whoever has the secret can mint `ROLE_ADMIN`
- **≥ 32 bytes** for HS256 — shorter, and JJWT stops startup with `WeakKeyException`

---

# `JwtService` — make a token

```java
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
```

---

# `JwtService` — read a token

```java
public String extractUsername(String token) {
  return parse(token).getSubject();
}

private Claims parse(String token) {
  return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
}
```

`parseSignedClaims` checks **signature and expiry first**:

| Token | Throws |
|---|---|
| edited payload | `SignatureException` |
| past `exp` | `ExpiredJwtException` |
| not a JWT | `MalformedJwtException` |

<div class="opacity-70 text-sm">All three are <code>JwtException</code> — the filter catches that one type.</div>

---

# Logging in needs Spring's machinery

```java
@Bean
AuthenticationProvider authenticationProvider() {
  DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
  provider.setPasswordEncoder(passwordEncoder());
  return provider;
}

@Bean
AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
  return config.getAuthenticationManager();
}
```

<div class="grid grid-cols-2 gap-4 mt-2">
<div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10"><b>Provider</b> — knows <i>how</i>: load the user, compare with BCrypt</div>
<div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10"><b>Manager</b> — the front door your service calls</div>
</div>

---

# How the login pieces fit

<div class="flex items-center justify-center gap-3 mt-6">
<div class="px-4 py-3 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-center"><code>login()</code><br><span class="text-sm">your service</span></div>
<div class="text-2xl opacity-60">→</div>
<div class="px-4 py-3 rounded-lg border-2 border-amber-500 bg-amber-500/10 text-center"><code>AuthenticationManager</code><br><span class="text-sm">the front door</span></div>
<div class="text-2xl opacity-60">→</div>
<div class="px-4 py-3 rounded-lg border-2 border-amber-500 bg-amber-500/10 text-center"><code>AuthenticationProvider</code><br><span class="text-sm">does the checking</span></div>
</div>

<div class="text-center text-sm opacity-70 mt-3">the provider asks two questions</div>

<div class="grid grid-cols-2 gap-8 mt-3 px-16">
<div class="px-4 py-3 rounded-lg border-2 border-green-600 bg-green-500/10 text-center"><code>UserDetailsService</code><br><b>"Who is this?"</b><br><span class="text-sm">→ the <code>AppUser</code> from the database</span></div>
<div class="px-4 py-3 rounded-lg border-2 border-purple-500 bg-purple-500/10 text-center"><code>PasswordEncoder</code><br><b>"Does the password match?"</b><br><span class="text-sm">→ yes or no</span></div>
</div>

<div class="text-center mt-6"><b>Yes</b> → logged in, make the token &nbsp;·&nbsp; <b>No</b> → 401</div>

---

# Who declares which piece

| Piece | Its one job | Where it comes from |
|---|---|---|
| `AuthenticationManager` | the front door: `authenticate(attempt)` | a `@Bean` in `SecurityConfig`, built by Spring from the providers |
| `DaoAuthenticationProvider` | check a username + password | a `@Bean` in `SecurityConfig` — you hand it the `UserDetailsService` and the `PasswordEncoder` |
| `UserDetailsService` | find a user by login name | your `CustomUserDetailsService`, a `@Service` |
| `UserDetails` | what Spring needs to know about a user | your `AppUser` entity — `implements UserDetails` |
| `PasswordEncoder` | hash, and compare with a hash | a `@Bean` in `SecurityConfig`: `BCryptPasswordEncoder` |

<div class="mt-4 p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-sm">
The JWT filter uses only <b>one</b> of them: <code>UserDetailsService</code>. A token is checked by its signature — no password, so no encoder, no manager, no provider.
</div>

---

# The login

```java
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
```

<div class="mt-4 opacity-80">

Email and password in — the `LoginRequest` stays in the controller. An `AuthResponse` out, because a token is never stored: there is no entity to return.

</div>

---

# Wrong password, unknown email: the same 401

<div class="grid grid-cols-2 gap-4 mt-4">
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

If the API said **"no such user"** for one and **"wrong password"** for the other…

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

…anyone could check which emails have accounts. Spring throws `BadCredentialsException` for **both**.

</div>
</div>

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

---
layout: section
---

# Checkpoint 4

Log in once, get a token

---

# The brief

<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-xl mt-4">
"Log in once on the phone — don't make the app keep the password."
</div>

| In the README | What |
|---|---|
| The dependencies, the properties | JJWT, `app.jwt.*` |
| `JwtService` | make a token, read a token |
| An `AuthenticationManager` | `AuthenticationProvider` + `AuthenticationManager` beans |
| The DTOs, the service, the endpoint | `LoginRequest`, `AuthResponse`, `login(email, password)` |
| A wrong password is a 401 | the 401 handler |

---

# Check

```bash
curl -i -H "Content-Type: application/json" \
  -d '{"email":"dara@sunrisekitchen.com","password":"cook1234"}' http://localhost:8080/auth/login
# 200 {"token":"eyJhbGciOiJIUzI1NiJ9…","tokenType":"Bearer","expiresIn":900,"email":"dara@…"}

# wrong password → 401 "Wrong email or password"
```

Paste it into **jwt.io** — `"sub":"dara@sunrisekitchen.com"`, `"role":"ROLE_USER"`.

<v-click>

Now **use** it on `POST /recipes` → **401**. The API hands out tokens — nothing reads them yet.

</v-click>

---

# Checkpoint 4

- Dara gets a token
- a wrong password → **401**
- jwt.io shows the claims
- the token is not accepted… **yet**

---
layout: section
---

# Accepting the token

The filter you write

---

# `JwtAuthenticationFilter`

```java {all|5-10|11-18|19-23}
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
```

---

# The filter, as a picture

<div class="grid grid-cols-[22rem_1fr] gap-x-4 gap-y-1 items-center mt-2 text-sm">
<div class="text-center px-3 py-1 rounded-lg border-2 border-gray-500 bg-gray-500/10 font-bold">Request</div><div></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div><div></div>
<div class="text-center px-3 py-1 rounded-lg border-2 border-amber-500 bg-amber-500/10"><code>Authorization: Bearer …</code> present?</div><div class="flex items-center gap-2"><span class="text-xl opacity-60">→</span><div class="px-3 py-1 rounded-lg border-2 border-dashed border-gray-400">no — carry on as <b>anonymous</b></div></div>
<div class="text-center text-xl opacity-60 leading-none">↓ <span class="text-xs">yes</span></div><div></div>
<div class="text-center px-3 py-1 rounded-lg border-2 border-amber-500 bg-amber-500/10">signature and expiry OK?</div><div class="flex items-center gap-2"><span class="text-xl opacity-60">→</span><div class="px-3 py-1 rounded-lg border-2 border-dashed border-gray-400">no — stay <b>anonymous</b></div></div>
<div class="text-center text-xl opacity-60 leading-none">↓ <span class="text-xs">yes</span></div><div></div>
<div class="text-center px-3 py-1 rounded-lg border-2 border-blue-500 bg-blue-500/10">load the user from the database</div><div></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div><div></div>
<div class="text-center px-3 py-1 rounded-lg border-2 border-blue-500 bg-blue-500/10">put them in the <code>SecurityContext</code></div><div></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div><div></div>
<div class="text-center px-3 py-1 rounded-lg border-2 border-green-600 bg-green-500/10">carry on → <code>AuthorizationFilter</code> → controller</div><div></div>
</div>

<div class="mt-2 p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10">
The filter <b>never answers by itself</b>. The rules and the entry point turn "anonymous" into 401 where it matters — and a public <code>GET</code> still works.
</div>

---

# Why load the user every time?

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Costs**

One database query per request.

</div>
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Buys**

A deleted user is out **at once**. A changed role counts on the **next** request.

</div>
</div>

<div class="opacity-70 text-sm mt-4">Systems that skip the lookup trust the token's claims until it expires — faster, and less control.</div>

---

# Wiring it in

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // … the rules from checkpoint 2 …
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
```

<div class="opacity-70 text-sm"><b>Before</b> Spring's username-and-password filter — so the user is known when the rules are checked.</div>

---

# `GET /me` — keep the controller clean

```java
@GetMapping("/me")
public ResponseEntity<UserResponse> me() {
  return ResponseEntity.ok(userMapper.toUserResponse(userService.getCurrentUser()));
}
```

<div class="grid grid-cols-3 gap-4 mt-6 text-sm">
<div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10"><b>Controller</b><br>HTTP only — a URL, a <code>200</code>, the mapper. No security code</div>
<div class="p-3 rounded-lg border-2 border-green-500 bg-green-500/10"><b>Service</b><br>answers <i>who is signed in?</i></div>
<div class="p-3 rounded-lg border-2 border-purple-500 bg-purple-500/10"><b>Mapper</b><br><code>AppUser</code> → <code>UserResponse</code> — no hash</div>
</div>

<div class="mt-4 opacity-80">No rule of its own — <code>anyRequest().authenticated()</code> covers it.</div>

---

# The service reads the `SecurityContext`

```java {all|3|4-5}
@Override
public AppUser getCurrentUser() {
  Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
  // The principal is the UserDetails the filter loaded -- our own AppUser
  return (AppUser) authentication.getPrincipal();
}
```

<v-click>

<div class="grid grid-cols-[1fr_auto_1fr_auto_1fr] items-center gap-2 mt-4 text-sm text-center">
<div class="p-2 rounded-lg border-2 border-amber-500 bg-amber-500/10"><code>JwtAuthenticationFilter</code><br>loads the <code>AppUser</code>, puts it in</div>
<div class="text-xl opacity-60">→</div>
<div class="p-2 rounded-lg border-2 border-blue-500 bg-blue-500/10 font-mono">SecurityContext</div>
<div class="text-xl opacity-60">→</div>
<div class="p-2 rounded-lg border-2 border-green-500 bg-green-500/10"><code>UserServiceImpl</code><br>takes the same <code>AppUser</code> out — no query</div>
</div>

<div class="mt-3 text-sm opacity-80">One <code>SecurityContext</code> <b>per request thread</b> — the filter, the controller and the service all see the same user.</div>

</v-click>

---

# The price of reading it in the service

<div class="grid grid-cols-2 gap-6 mt-4">
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Buys**

A controller with no security code — every endpoint looks the same.

</div>
<div class="p-4 rounded-lg border-2 border-amber-500 bg-amber-500/10">

**Costs**

`getCurrentUser()` only works **inside a signed-in request**. A scheduled job, or a test nobody signed into → `getAuthentication()` is `null`.

</div>
</div>

<div class="mt-4 opacity-80 text-sm">In a test, <code>@WithUserDetails("dara@…")</code> loads the real <code>AppUser</code>. <code>@WithMockUser</code> puts Spring's own <code>User</code> there — and the cast fails.</div>

---

# Pause

After checkpoint 5, someone sends checkpoint 1's HTTP Basic header:

```bash
curl -i -u dara@sunrisekitchen.com:cook1234 -X POST … http://localhost:8080/recipes
```

<v-click>

<div class="mt-4 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
<b>401.</b> No filter in the chain reads <code>Basic</code> any more. Your filter only reads <code>Bearer</code>.
</div>

</v-click>

---
layout: section
---

# Checkpoint 5

Accept the token

---

# The brief

<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-xl mt-4">
"So — the app sends that token, and it's in."
</div>

| In the README | What |
|---|---|
| The filter | `security/JwtAuthenticationFilter` |
| Put it in the chain | `SecurityConfig` with `.addFilterBefore(...)` |
| `GET /me` | `UserService.getCurrentUser()` with `SecurityContextHolder`, `UserController` |

---

# Check

```bash
DARA=eyJ…     SOKHA=eyJ…        # from /auth/login

curl -i -H "Authorization: Bearer $DARA" http://localhost:8080/me
# 200 {"id":2,"email":"dara@sunrisekitchen.com","fullName":"Dara Sok","role":"USER"}

curl -i http://localhost:8080/me
# 401

curl -i -H "Authorization: Bearer $DARA" -H "Content-Type: application/json" -d '{…}' \
  http://localhost:8080/recipes
# 201   ← note your id
```

---

# Break it on purpose — forge a token

1. jwt.io: change `ROLE_USER` → `ROLE_ADMIN`
2. Use the new token on `/me`

<v-click>

```
401
```

In the filter: `SignatureException: JWT signature does not match locally computed signature`.

Anyone can **read** a JWT. Only the secret's holder can **write** one.

</v-click>

---

# Break it on purpose — watch it expire

1. `app.jwt.expiration-ms=30000`, restart, log in
2. Call `/me` — then again after 30 seconds

<v-click>

```
401   — ExpiredJwtException, caught in the filter
```

JJWT allows **no clock skew** by default: 30 seconds means 30 seconds. **Set it back to `900000`.**

</v-click>

---

# Checkpoint 5

- `/me` answers for Dara with her token
- Dara can create a recipe
- forged or expired token → **401**

---
layout: section
---

# Who may delete

On the URL first — then on the action

---

# A rule on the URL — `hasRole`

<div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-500/10 mt-2">
The rule: "Only the head chef can <b>delete a recipe</b>."
</div>

```java {5}
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/recipes/**", "/tags/**").permitAll()
    .requestMatchers("/auth/**", "/error").permitAll()
    // Only the head chef deletes
    .requestMatchers(HttpMethod.DELETE, "/recipes/**").hasRole("ADMIN")
    .anyRequest().authenticated())
```

<v-click>

**Above** `anyRequest()` — first match wins. Nobody **401**, Dara **403**, Sokha **204**.

</v-click>

---

# The second door

A month later: *"The mobile team wants a 'remove' button."*

```java
@PostMapping("/{id}/remove")
public ResponseEntity<Void> removeRecipe(@PathVariable Long id) {
  recipeService.delete(id);
  return ResponseEntity.noContent().build();
}
```

<v-click>

```bash
curl -i -X POST -H "Authorization: Bearer $DARA" http://localhost:8080/recipes/<id>/remove
# 204   ← Dara just deleted a recipe
```

</v-click>

<v-click>

<div class="mt-2 p-3 rounded-lg border-2 border-red-500 bg-red-500/10">
Nothing is broken. The rule guards <code>DELETE /recipes/**</code> — this is a <code>POST</code>. <b>The rule was on a door. The client's rule is about the action.</b>
</div>

</v-click>

---

# A rule on the action — `@PreAuthorize`

Every door ends in one method. Put the rule there.

```java
@Override
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public void delete(Long id) {
  recipeRepository.deleteById(id);
}
```

<div class="grid grid-cols-2 gap-4 mt-4 text-sm">
<div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10"><code>@EnableMethodSecurity</code> on <code>SecurityConfig</code> switches it on</div>
<div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-500/10">The <code>DELETE</code> URL line comes out — one rule, one home</div>
</div>

Checked **before** the method runs, whoever called it. Both doors: Dara **403**.

---

# URL rule or method rule?

| | `hasRole(...)` in the chain | `@PreAuthorize` |
|---|---|---|
| Guards | one HTTP method on a URL pattern | one Java method, from any caller |
| Runs | in the filter chain, before the controller | on the service's proxy |
| Good for | broad strokes — "writes need a signed-in user" | business rules — "only the head chef deletes" |
| Misses | the same action reached another way | calls from inside the same class |

---

# What `@PreAuthorize` can say

| Expression | Means |
|---|---|
| `hasRole('ADMIN')` | has `ROLE_ADMIN` |
| `hasAnyRole('ADMIN', 'EDITOR')` | either |
| `isAuthenticated()` | anybody signed in |
| `#email == authentication.name` | the argument is the caller's own email |

---

# It is an aspect — Session 8

<div class="flex items-stretch justify-center gap-2 mt-4 text-sm text-center">
<div class="flex items-center px-4 rounded-lg border-2 border-green-600 bg-green-500/10 font-bold">controller</div>
<div class="flex items-center text-2xl opacity-60">→</div>
<div class="p-3 rounded-lg border-2 border-dashed border-gray-500 w-80">
<div class="font-mono font-bold">proxy of RecipeServiceImpl</div>
<div class="text-xs opacity-70">generated — like Session 8's aspect</div>
<div class="mt-2 px-2 py-1 rounded-lg border-2 border-amber-500 bg-amber-500/10"><code>ROLE_ADMIN</code> in the <code>SecurityContext</code>?</div>
<div class="grid grid-cols-2 gap-2 mt-2 text-xs">
<div class="p-2 rounded-lg border-2 border-red-500 bg-red-500/10"><b>no</b><br><code>AccessDeniedException</code> → 403</div>
<div class="p-2 rounded-lg border-2 border-green-600 bg-green-500/10"><b>yes</b><br>call the real method →</div>
</div>
</div>
<div class="flex items-center text-2xl opacity-60">→</div>
<div class="flex flex-col justify-center px-4 rounded-lg border-2 border-blue-500 bg-blue-500/10"><span class="font-mono font-bold">RecipeServiceImpl</span><span class="font-mono">.delete(…)</span></div>
</div>

<div class="mt-2 p-3 rounded-lg border-2 border-red-500 bg-red-500/10">
Same limits: <code>this.delete(id)</code> from inside the class <b>skips</b> the proxy — and the check.
</div>

---

# Pause

```java
public void cleanUpOld() {
  for (Long id : findOldRecipeIds()) {
    delete(id);
  }
}
```

Dara calls the endpoint that runs this. Is she stopped?

<v-click>

<div class="mt-4 p-3 rounded-lg border-2 border-green-600 bg-green-500/10">
<b>No.</b> A self-call — no proxy, no check. Put <code>@PreAuthorize("hasRole('ADMIN')")</code> on <code>cleanUpOld()</code> too.
</div>

</v-click>

---

# Why 403 and not 500?

<div class="flex flex-col gap-1 mt-4 text-sm">
<div class="grid grid-cols-[18rem_1fr] items-center px-4 py-2 rounded-lg border-2 border-red-500 bg-red-500/10"><span class="font-mono font-bold">RecipeServiceImpl.delete</span><span>throws <code>AccessDeniedException</code></span></div>
<div class="text-center text-xl opacity-60 leading-none">↓ <span class="text-xs">travels up</span></div>
<div class="grid grid-cols-[18rem_1fr] items-center px-4 py-2 rounded-lg border-2 border-gray-400"><span class="font-mono font-bold">GlobalExceptionHandler</span><span>handles specific exceptions only — <b>passes it on</b></span></div>
<div class="text-center text-xl opacity-60 leading-none">↓</div>
<div class="grid grid-cols-[18rem_1fr] items-center px-4 py-2 rounded-lg border-2 border-amber-500 bg-amber-500/10"><span class="font-mono font-bold">ExceptionTranslationFilter</span><span>turns it into <b>403</b></span></div>
</div>

<div class="mt-4 p-3 rounded-lg border-2 border-red-500 bg-red-500/10">
Add a catch-all <code>@ExceptionHandler(Exception.class)</code> one day, and it grabs the denial first → <b>500</b>.
</div>

---
layout: section
---

# Checkpoint 6

Only the head chef deletes

---

# The brief and the check

<div class="p-5 rounded-lg border-2 border-blue-500 bg-blue-500/10 text-lg mt-2">
"Deleting a recipe is final. Only the head chef can do that."
</div>

1. The `hasRole` line → `401` · `403` · `204`
2. Break it on purpose: the `/remove` door → Dara gets `204`
3. `@EnableMethodSecurity`, `@PreAuthorize`, drop the URL line → both doors `403` for Dara
4. Delete the `/remove` endpoint

```bash
curl -i -X DELETE http://localhost:8080/recipes/<id>                                # 401
curl -i -X DELETE -H "Authorization: Bearer $DARA"  http://localhost:8080/recipes/<id>   # 403
curl -i -X DELETE -H "Authorization: Bearer $SOKHA" http://localhost:8080/recipes/<id>   # 204
```

---

# Where you ended up

| Request | Nobody | Dara (`USER`) | Sokha (`ADMIN`) |
|---|---|---|---|
| `POST /auth/register`, `/auth/login` | open | open | open |
| `GET` on `/recipes/**`, `/tags/**` | 200 | 200 | 200 |
| `POST` / `PUT /recipes` | 401 | 201 / 200 | 201 / 200 |
| `DELETE /recipes/{id}` | 401 | **403** | 204 |
| `GET /me` | 401 | 200 | 200 |
| forged or expired token | 401 | 401 | 401 |

---
layout: section
---

# JWT practices

---

# Do and don't

<div class="grid grid-cols-2 gap-4 mt-4">
<div class="p-4 rounded-lg border-2 border-green-600 bg-green-500/10">

**Do**
- HTTPS — a token is as good as a password while it lives
- short access tokens — 15 minutes
- the secret in an environment variable
- check signature **and** expiry

</div>
<div class="p-4 rounded-lg border-2 border-red-500 bg-red-500/10">

**Don't**
- secrets in the payload
- a short, guessable secret
- log tokens
- trust a claim you didn't check

</div>
</div>

---

# Where the client keeps it

| Storage | XSS can read it? | Sent automatically? | Verdict |
|---|---|---|---|
| `localStorage` | yes | no | avoid |
| `sessionStorage` | yes | no | better |
| `HttpOnly` cookie | no | **yes** — CSRF is back | good, with CSRF on |
| memory + refresh token | no | no | **best** |

<div class="opacity-70 text-sm mt-2">The last row needs a refresh token — Session 12.</div>

---
layout: section
---

# What you have *not* learned

---

# Not finished — yet

| Missing | Why | Where |
|---|---|---|
| **Refresh tokens** | after 15 minutes Dara types her password again | Session 12 |
| **Logout** | nothing can cancel a token before it expires | Session 12 |
| **CORS** | a browser app on another port can't call the API | Session 12, shown |
| **Error bodies** | a 401 from the filter is an empty body | Session 12, shown |
| **OAuth2** | log in with GitHub — GitHub checks the password, not us | Session 12 |
| **HTTPS** | without it the token travels in clear | Session 12, shipping |
| **Rate limiting** | nothing stops a thousand guesses a minute | — |

---

# The failure mode to avoid

<div class="p-6 rounded-lg border-2 border-red-500 bg-red-500/10 mt-6 text-xl">
A developer who can put <code>@PreAuthorize</code> on a method,<br>but can't say whether an endpoint is protected.
</div>

<div class="mt-6">

You can now:

1. follow the request through the chain
2. name the filter that answers
3. prove it with `curl`

</div>

---
layout: section
---

# Recap

---

# Recap

- **Authentication** = who are you (**401**). **Authorisation** = are you allowed (**403**)
- The starter **locks everything**; you open what should be open
- Requests pass the **filter chain before** your controller
- **First match wins**, `anyRequest()` last · CSRF off, `STATELESS` · an **entry point** for 401
- Users from a `UserDetailsService` · **BCrypt** · nobody registers as admin
- A **JWT** is readable by anyone, changeable by no one
- **Your filter** reads `Bearer`, checks, loads, fills the `SecurityContext` — and never answers itself
- `@PreAuthorize` guards the **action** — an aspect on a proxy

---
layout: center
class: text-center
---

# Next session — tokens that last, GitHub login, then shipping

Refresh tokens · logout · log in with GitHub

Then the Recipe API and PostgreSQL go into containers.

<div class="opacity-70 mt-6">Read <code>lecture.md</code> tonight. The extra section shows how to test today's rules.</div>
