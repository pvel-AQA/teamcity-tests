# Architecture Review — TeamCity Test Framework

**Scope:** 142 Java types (124 in `src/main/java`, 18 in `src/test/java`), 371 intra-project imports.
**Method:** every conclusion below is derived from the real `import` graph produced by
`scripts/generate_uml.py`, not from intent or naming. Nothing here is hand-maintained — re-run the
script after any refactor and the numbers move with the code.

**Question this answers:** *has low coupling been achieved by organising classes into layers with
clear responsibilities and minimal dependencies on each other?*

**Short answer:** yes at the bottom of the stack, no in the middle. The data layer, enums, config and
HTTP transport are genuinely well isolated. The `Steps`, `PageObjects` and `Hooks` band is knotted:
three dependency cycles, five upward edges, and a steps layer that tests bypass 4:1.

---

## 1. Diagram index

| File | What it shows | Read it to answer |
| --- | --- | --- |
| [`01-packages.puml`](01-packages.puml) | Layer-to-layer dependency graph. Edge labels = number of import statements. **Red bold = edge pointing up the stack.** | *Is the layering respected?* Start here. |
| [`02-classes.puml`](02-classes.puml) | All 142 types grouped into layer packages, inheritance edges only. | *What is the class inventory and the type hierarchy?* |
| [`03-transport.puml`](03-transport.puml) | `HttpRequest` / `CrudRequester` / `ValidatedCrudRequester` / `Endpoint` / specs, with external deps in grey. | *Is the HTTP core clean?* (It is — the model of what the rest should look like.) |
| [`03-steps.puml`](03-steps.puml) | `UserSteps`, `SuperUserSteps` and everything they touch. | *Why is Steps the most volatile layer?* |
| [`03-pageobjects.puml`](03-pageobjects.puml) | `BasePage` + 16 pages, `BaseElement` + `ProjectElement`. | *Where does the UI stack leak into the API stack?* |
| [`03-hooks.puml`](03-hooks.puml) | 6 annotations ↔ 7 JUnit extensions. | *Is the annotation→extension pattern applied consistently?* |
| [`03-models.puml`](03-models.puml) | 48 DTOs under `BaseModel`. | *Is the data layer dependency-free?* (Almost.) |
| [`03-test.puml`](03-test.puml) | `BaseTest` → `BaseUiTest` → `SingleThreadBaseTest` and the 15 test classes. | *Do tests inherit the right base?* |
| [`03-generators.puml`](03-generators.puml), [`03-helpers.puml`](03-helpers.puml), [`03-enums.puml`](03-enums.puml), [`03-config.puml`](03-config.puml), [`03-comparison.puml`](03-comparison.puml), [`03-database.puml`](03-database.puml) | Support layers. | Detail views. |
| [`coupling.md`](coupling.md) | Generated Ce/Ca/Instability table, cycle list, upward-edge list. | *The raw numbers.* |

**Render:** `brew install plantuml && plantuml uml/*.puml` → PNGs next to each source.
Or paste into <https://www.plantuml.com/plantuml>. Or IntelliJ + *PlantUML Integration* plugin.

---

## 2. The layer model

Nine layers. Dependencies are supposed to flow strictly downward.

```
Tests            src/test/java · scenarios and assertions
  ↓
Hooks            common.annotations + common.extensions · declarative setup/teardown
  ↓
Steps            api.steps · business actions (createProject, runBuild, …)
Page Objects     ui.pages + ui.elements · screens, Selenide
  ↓
Generators       api.generators · random data driven by @GeneratingRule
  ↓
Transport        api.request.skelethon + api.specs · HttpRequest → CrudRequester, Endpoint, specs
  ↓
Models           api.models · 48 DTOs, all extending BaseModel
  ↓
Support          common.helpers · EntityStorage, StepLogger, WaitUtils, RetryUtils
Config           common.configs.Config
Enums            api.enums, ui.enums, common.enums
```

**How they connect.** A test declares intent with an annotation (`@AuthUser`); the matching JUnit
extension performs the setup by calling a step. The step builds a model via a generator and hands it
to a `ValidatedCrudRequester` bound to an `Endpoint` constant. `Endpoint` is the single place where a
URL, its request model and its response model are tied together — adding an endpoint costs one enum
constant and nothing else. The requester wraps RestAssured using a spec from `api.specs`, logs the
call through `StepLogger`, and registers anything it created in `EntityStorage` so teardown is
automatic. UI tests follow the same path but enter through a page object instead of a step.

---

## 3. Coupling scoreboard

Ce = outgoing imports, Ca = incoming, **I = Ce/(Ce+Ca)**. I near 0 means *stable* — lots of things
depend on it, it depends on little, so it is safe to build on. I near 1 means *volatile*. Healthy
layering shows I decreasing as you go down.

| Layer | Ce | Ca | I | Reading |
| --- | ---: | ---: | ---: | --- |
| Test | 177 | 0 | 1.00 | ✅ correct — nothing depends on tests |
| Database | 2 | 0 | 1.00 | ⚠️ dead code, not a layer |
| **Steps** | 40 | 18 | **0.69** | ⚠️ volatile *and* depended upon — worst quadrant |
| **PageObjects** | 18 | 12 | **0.60** | ⚠️ should be ~0.3; leaks into the API stack |
| Hooks | 15 | 22 | 0.41 | ⚠️ sits both above and below other layers |
| Transport | 23 | 56 | 0.29 | ✅ |
| Generators | 5 | 24 | 0.17 | ✅ apart from one call back up into Steps |
| Helpers | 3 | 15 | 0.17 | ⚠️ its 3 outgoing edges all point up into Transport |
| Models | 11 | 60 | 0.15 | ✅ |
| Comparison | 0 | 5 | 0.00 | ✅ |
| Config | 0 | 5 | 0.00 | ✅ |
| Enums | 0 | 52 | 0.00 | ✅ perfectly stable |

The shape is right at both ends: Enums/Config/Models/Transport are stable, Tests have Ca = 0 so
nothing depends upward on them. **Every problem is in the middle band.**

---

## 4. What is good, and why

**① The HTTP transport core — see [`03-transport.puml`](03-transport.puml).**
`HttpRequest` (abstract, owns URL templating and path/query resolution) → `CrudRequester` (raw
`ValidatableResponse`) and `ValidatedCrudRequester<T>` (deserialised model), both behind two narrow
interfaces `CrudEndpointInterface` and `GetAllEndpointInterface`. `Endpoint` binds URL + request
model + response model in one enum constant.
*Why it's good:* I = 0.29 with Ca = 56 — the most depended-upon executable code in the project, and
it barely depends on anything. A new endpoint is one enum line, zero new classes. The
raw/validated split means a negative test can assert on a 400 without fighting deserialisation.

**② The data layer — see [`03-models.puml`](03-models.puml).**
48 DTOs, all extending an empty `BaseModel` marker, Lombok-generated accessors, I = 0.15.
*Why it's good:* the marker type is what lets `CrudEndpointInterface` accept any body without
generics gymnastics, and models carry no behaviour, so they can't drag logic across layers.

**③ Enums and config are perfectly stable — I = 0.00, Ca = 52 and 5.**
*Why it's good:* zero outgoing edges means they can never participate in a cycle. Error messages,
locators, build states and UI text are centralised rather than duplicated as string literals.

**④ Automatic cleanup via `EntityStorage`.**
`CrudRequester.post()` inspects the response for `href`/`id` and registers the created entity;
`BaseTest.afterEach` clears everything.
*Why it's good:* API tests have no manual teardown at all, and cleanup can't drift from creation
because the same code path does both.

**⑤ The annotation → extension hook pattern — see [`03-hooks.puml`](03-hooks.puml).**
`@AuthUser` is meta-annotated `@ExtendWith(AuthUserExtension.class)`; the test declares intent, the
extension owns the mechanics.
*Why it's good:* setup logic is reusable and invisible at the call site. The annotation↔extension
mutual reference is mandated by JUnit 5 — it is **not** a design flaw and should not be "fixed".

**⑥ Test base hierarchy is a clean chain.** `BaseTest` → `BaseUiTest` → `SingleThreadBaseTest`, each
adding exactly one concern (soft assertions + storage → Selenide config → single-thread execution).

---

## 5. What needs improvement, and why

Ordered by how much damage each one does. Red edges in [`01-packages.puml`](01-packages.puml)
correspond to items ①②③⑤.

### ① `Steps` is a shortcut, not a boundary — *the single biggest issue*

**Evidence:** 42 direct imports of `api.request.*` / `api.specs` from test classes vs 11 imports of
`api.steps`. Bypassing classes: all 8 API tests plus `ui/AuthenticationTest`, `ui/LoginTest`,
`ui/UsersEnterWithoutLoginTest`.

**Why it matters:** the whole point of a steps layer is to be the *only* business-facing surface, so
an endpoint or payload change stops at `UserSteps` instead of rippling into test bodies. At a 4:1
bypass ratio it provides no isolation — it is an optional convenience helper. This is also why Steps
sits at I = 0.69: it depends on 40 things while 18 depend on it, so it is simultaneously the most
fragile and one of the more depended-upon layers.

**Fix:** route test bodies through `UserSteps`, adding step methods where they are missing. This is
the only item here that can't be done in one sitting — do it incrementally, and treat "no new
`api.request` import in `src/test`" as the ratchet.

### ② A generator performs a live HTTP call

**Evidence:** `src/main/java/api/generators/TeamCityDataGenerator.java:15` —
`generateBuildConfigurationFor()` calls `UserSteps.createProject()`.

**Why it matters:** this is the `Generators → Steps` cycle in [`coupling.md`](coupling.md). A caller
reading `generateBuildConfigurationFor()` cannot tell that it creates server-side state and needs a
live TeamCity. Every other `generate*` method in the class is pure. That inconsistency makes
generators unusable for offline or negative cases, and it drags the whole generator layer's
reachable dependency set up to include transport and specs.

**Fix:** split it. Pure builders stay in `TeamCityDataGenerator`; anything creating server state moves
to `UserSteps` (which already imports the generator, so the edge becomes one-directional).

### ③ Transport depends on a JUnit extension — the stack fully reverses

**Evidence:** `src/main/java/api/specs/RequestSpec.java:9,93` —
`.setAuth(RestAssured.oauth2(AuthUserExtension.getAuthUserToken().getValue()))`.

**Why it matters:** the lowest executable layer reaches into the test-lifecycle layer. `RequestSpec`
can no longer be used outside a JUnit run — not from a script, not from a fixture, not from `main()`.
It also closes the loop `extensions → steps → specs → extensions`, so a change in any of the three
can surprise the other two.

**Fix (do this one first — ~1h, highest value per hour):** have `AuthUserExtension` push the token
into a `ThreadLocal` holder in `common.helpers`; `RequestSpec` reads the holder. The extension keeps
writing, transport keeps reading, and the upward edge disappears.

### ④ API and UI stacks are not separated

**Evidence, four independent leaks:**

| Leak | Location |
| --- | --- |
| Base page object calls the API spec layer | `ui/pages/BasePage.java:3,35` — `RequestSpec.setCookieInBrowser(RequestSpec.fetchSessionCookie(...))` |
| A JUnit extension imports a page object | `common/extensions/AuthUserExtension.java:11` — `import ui.pages.BasePage` |
| Two **API** tests extend the **UI** base class | `api/agent/AgentTest.java:22` and `api/buildRun/RegularBuildRunTest.java:25` — `extends SingleThreadBaseTest` |
| UI dropdown enums live under `api.enums` | `api/enums/buildconfiguration/BuildConfigDropdown`, `BuildConfigTypeDropdown` — literal UI labels ("From template", "Regular") used only by `ui.pages` |

**Why it matters:** there is no single edge you could cut to separate the two stacks — which is the
practical test of whether they *are* two stacks. Concretely: cookie-based login is test setup, not a
base-page concern, and putting it on `BasePage` places `RequestSpec` on the dependency path of all 17
pages (that's the `PageObjects → Transport` edge and most of why I = 0.60). The two API tests inherit
Selenide config, remote browser setup and `Selenide.closeWebDriver()` when all they wanted was
`@Execution(SAME_THREAD)` — so a Selenide or browser problem can now fail a pure API test.

**Fix:** move `authAsUser` off `BasePage` into a `LoginSteps` / `ui.base` helper; introduce
`SingleThreadApiTest extends BaseTest` and re-parent the two API tests (~15 min); move
`api.enums.buildconfiguration.*` → `ui.enums.buildconfiguration` (~15 min, pure package move).

### ⑤ `Helpers` depends upward on `Transport`

**Evidence:** `common/helpers/EntityStorage.java:3-5` imports `Endpoint`, `CrudRequester`,
`RequestSpec`. Meanwhile `CrudRequester` imports `EntityStorage` and `StepLogger`, and
`RequestSpec:141` imports `DockerLogsExtractor`.

**Why it matters:** `common.helpers` reads as a generic utility package — the kind of thing anything
may safely depend on. It isn't: it drags in RestAssured and the whole endpoint enum. This is the
`Helpers ↔ Transport` cycle, and it's why the auto-cleanup mechanism (genuinely one of the better
features here) can't be reused or tested in isolation.

**Fix:** put the cleanup HTTP call behind a small interface that `EntityStorage` depends on and
transport implements. Storage keeps a list of URLs and a deleter; it stops knowing what HTTP is.

### ⑥ `api.database` is dead code

**Evidence:** `DBService`, `DBRequest`, `DBHelper`, `Condition` — 303 lines, **Ca = 0**. Nothing in
the project references the package. It carries jOOQ 3.21.4 and HikariCP 7.0.2 (`pom.xml:14-15,89-93`).

**Why it matters:** two runtime dependencies, a connection-pool surface and 303 lines of
unexercised code on the maintenance and security-scan budget for zero test value. It also shows up
in [`01-packages.puml`](01-packages.puml) as a component with no inbound edges, which misleads anyone
reading the diagram into thinking DB verification exists.

**Fix:** either wire it into assertions (verify via DB what the API claims) or delete it with its two
dependencies. Deleting is fine — it's in git history.

### ⑦ Duplicate simple names across stacks

**Evidence:** `api.build.BuildConfigurationTest` vs `ui.buildconfiguration.BuildConfigurationTest`;
`api.buildRun.RegularBuildRunTest` vs `ui.buildRun.RegularBuildRunTest`.

**Why it matters:** ambiguous in Allure/surefire reports, in stack traces and in IDE navigation —
you cannot tell which stack failed without opening the file. It is also visible as a defect in
[`02-classes.puml`](02-classes.puml), where each pair collapses into a single node.

**Fix:** rename to `ApiBuildConfigurationTest` / `UiBuildConfigurationTest` (or equivalent).

### ⑧ Scratch file in the compiled test source root

**Evidence:** `src/test/java/ui/nk_Test_Ideas.java` — untracked, non-conventional name, no base class.
**Fix:** move to a scratch directory or delete.

---

## 6. Prioritised fix list

| # | Fix | Effort | Removes |
| --- | --- | --- | --- |
| 1 | `RequestSpec` reads a `ThreadLocal` token holder instead of calling `AuthUserExtension` | ~1h | upward edge ③, one cycle |
| 2 | Split `TeamCityDataGenerator`; state-creating methods move to `UserSteps` | ~2h | `Generators ↔ Steps` cycle ② |
| 3 | Move `authAsUser` off `BasePage` into `LoginSteps` | ~2h | `PageObjects → Transport` edge ④ |
| 4 | `SingleThreadApiTest extends BaseTest`; re-parent `AgentTest` + api `RegularBuildRunTest` | ~15 min | ④ |
| 5 | `EntityStorage` deletes through an interface, not `CrudRequester` | ~1h | `Helpers ↔ Transport` cycle ⑤ |
| 6 | `api.enums.buildconfiguration.*` → `ui.enums.buildconfiguration` | ~15 min | ④ |
| 7 | Rename duplicated test classes; remove `nk_Test_Ideas` | ~15 min | ⑦⑧ |
| 8 | Delete `api.database` + jOOQ/Hikari, or wire it into assertions | ~30 min | ⑥ |
| 9 | Route test bodies through `UserSteps` | incremental | ① — the one that actually creates a boundary |

Items 1–6 remove **every** cycle except the two harmless ones (JUnit's mandated
annotation↔extension, and the `BasePage ↔ BaseElement` pair which is mutual by design) and take the
API and UI stacks fully apart. Item 9 is the one that turns `Steps` into a real boundary.

---

## 7. Keeping this honest

```bash
python3 scripts/generate_uml.py     # regenerate all diagrams + coupling.md
plantuml uml/*.puml                 # render to PNG
```

Re-run after each fix: `Steps` I should fall from 0.69, `PageObjects` from 0.60, and the red edges
in `01-packages.puml` should disappear one at a time. That's the acceptance criterion — the diagram
is the test.

To stop regressions from re-entering, consider an **ArchUnit** test asserting the rules the diagram
implies: `api.*` must not depend on `ui.*`; `common.helpers` must not depend on `api.request`;
`api.specs` must not depend on `common.extensions`; layers must be free of cycles. That converts this
review from a snapshot into a build-time gate.
