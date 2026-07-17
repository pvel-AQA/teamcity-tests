# TeamCity Tests — Current Testing Pyramid

_Snapshot of the automated tests that exist in this repository as of 2026-07-17._

This pyramid is **descriptive** — it reflects the tests that are actually written and runnable today, not a target design. (For the aspirational P1 target, see `src/main/TeamCity_API_Test_Pyramid_P1.md`.)

## The shape today

```
                    /\
                   /  \        E2E — 1 test
                  /----\       BasicFlowsTest
                 /      \
                /        \     UI — 0 tests
               / (none)   \    (BaseUiTest scaffolding only)
              /------------\
             /              \
            /  API / INTEGRATION \   50 tests
           /  6 classes, single-  \  ConfigSteps, User, Project,
          /   endpoint + CRUD chains\ BuildConfiguration, Agent, Auth
         /--------------------------\
```

**Reality check:** the suite is **API-heavy** rather than a classic broad-based pyramid. The UI layer is scaffolding only, and E2E is a single deep scenario. Almost all coverage (50 of 51 methods, ~98%) sits in the API/integration band.

## Layer breakdown

| Layer | Tests | Classes | What it covers |
|-------|------:|---------|----------------|
| **E2E** (apex) | 1 | `api/e2e/BasicFlowsTest` | Full workflow: auth → project → build config → build step → queue → agent assignment → execution → result → build log |
| **UI** | 0 | `ui/BaseUiTest` (base only) | Selenide/Selenoid remote-browser setup exists, but no UI test classes yet |
| **API / Integration** (base) | 50 | 6 classes | Single-endpoint contract checks and short CRUD chains against the live REST API |
| **Total** | **51** | | (~53 executions incl. 3 parameterized cases) |

## API layer detail (the 50)

| Class | Tests | Focus |
|-------|------:|-------|
| `api/ConfigStepsTest` | 19 | Build-step CRUD, validation, and role/auth restrictions |
| `api/user/UserTest` | 12 | User CRUD, duplicate/missing-field validation, auth |
| `api/project/ProjectTest` | 7 | Project create/get/delete, bad locators, auth |
| `api/build/BuildConfigurationTest` | 5 | Build config create/copy/delete, duplicate/invalid id |
| `api/agent/AgentTest` | 3 | Agent connect / authorize / unauthorize lifecycle |
| `api/authorization/AuthorizationUserTest` | 4 | Basic & bearer auth, incl. 1 parameterized negative (3 cases) |

### API tests by intent

Even though every test lives in the API band, they split roughly into three equal concerns:

| Intent | Tests | Share | Examples |
|--------|------:|------:|----------|
| **Positive / happy-path** | 18 | 36% | `adminCanCreateUserTest`, `userCanCopyBuildConfiguration`, `agentCanBeAuthorizedTest` |
| **Negative / validation** | 15 | 30% | `adminCannotCreateUserWithDuplicateUsernameTest`, `userCannotCreateBuildConfigurationWithInvalidId`, `ConfigStepsCannotGetNonExistingStepTest` |
| **Security — auth & RBAC** | 17 | 34% | `ConfigStepsViewerCannotCreateStepTest`, `cannotCreateUserWithoutAuthTest`, `invalidBearerTokenAuthTest` |

## Observations & gaps

- **UI is scaffolding only.** `BaseUiTest` wires up Selenide + Selenoid but no UI test class uses it yet.
- **E2E is thin by design.** One deep scenario (`BasicFlowsTest`) is appropriate for the apex; keep it few and slow.
- **Strong security coverage.** ~34% of API tests assert auth/RBAC behaviour (401/403, role restrictions, invalid credentials/tokens) — unusually thorough for an API suite.
- **API-first shape.** Value is concentrated in the API/integration band, which forms the base of this pyramid. That is common and effective for API-first products where the REST surface is the primary contract under test.

---
_Counts are of `@Test`/`@ParameterizedTest` methods discovered under `src/test/java`. Parameterized cases add ~3 executions on top of the 51 methods._