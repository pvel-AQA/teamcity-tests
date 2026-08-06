# Coupling metrics (generated)

Ce = outgoing imports, Ca = incoming, I = Ce/(Ce+Ca).
I near 0 = stable (safe to depend on); I near 1 = volatile.
Healthy layering: I decreases as you go down the stack.

| Layer | Ce | Ca | I |
| --- | ---: | ---: | ---: |
| Test | 177 | 0 | 1.00 |
| Database | 2 | 0 | 1.00 |
| Steps | 40 | 18 | 0.69 |
| PageObjects | 18 | 12 | 0.60 |
| Hooks | 15 | 22 | 0.41 |
| Transport | 23 | 56 | 0.29 |
| Generators | 5 | 24 | 0.17 |
| Helpers | 3 | 15 | 0.17 |
| Models | 11 | 60 | 0.15 |
| Comparison | 0 | 5 | 0.00 |
| Config | 0 | 5 | 0.00 |
| Enums | 0 | 52 | 0.00 |
| Other | 0 | 25 | 0.00 |

## Cycles between layers

| A | B | A->B | B->A |
| --- | --- | ---: | ---: |
| Generators | Models | 3 | 7 |
| Generators | Steps | 1 | 3 |
| Helpers | Transport | 3 | 3 |

## Upward dependencies (lower layer importing a higher one)

| From | To | Imports |
| --- | --- | ---: |
| Models | Generators | 7 |
| Helpers | Transport | 3 |
| Database | Models | 1 |
| Generators | Steps | 1 |
| Transport | Hooks | 1 |
