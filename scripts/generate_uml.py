#!/usr/bin/env python3
"""Generate PlantUML diagrams + coupling metrics from the Java sources.

Parses every .java file under src/ for its package, type declaration,
inheritance and intra-project imports, then emits:

  uml/01-packages.puml   package-level dependency graph (edge labels = #imports)
  uml/02-classes.puml    every class grouped by layer, inheritance edges only
  uml/03-<layer>.puml    one detail diagram per layer (inheritance + usage)
  uml/coupling.md        Ce / Ca / Instability per layer + cycle list

Usage:  python3 scripts/generate_uml.py
Render: https://www.plantuml.com/plantuml  |  brew install plantuml && plantuml uml/*.puml
        |  IntelliJ + PlantUML Integration plugin (opens .puml directly)
"""

import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src"
OUT = ROOT / "uml"

# --- layer mapping: first matching prefix wins, so order matters -------------
LAYERS = [
    ("Test",       ["base", "api.project", "api.agent", "api.build", "api.buildRun",
                    "api.authorization", "api.ConfigStepsTest", "ui.base", "ui.project",
                    "ui.buildconfiguration", "ui.buildRun"]),
    ("Hooks",      ["common.annotations", "common.extensions"]),
    ("Steps",      ["api.steps"]),
    ("Generators", ["api.generators"]),
    ("PageObjects", ["ui.pages", "ui.elements"]),
    ("Transport",  ["api.request", "api.specs"]),
    ("Models",     ["api.models"]),
    ("Database",   ["api.database"]),
    ("Comparison", ["api.comparison"]),
    ("Helpers",    ["common.helpers"]),
    ("Config",     ["common.configs"]),
    ("Enums",      ["api.enums", "ui.enums", "common.enums"]),
]

TYPE_RE = re.compile(
    r"^\s*(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+|abstract\s+)*"
    r"(class|interface|enum|@interface)\s+(\w+)"
    r"(?:\s*<[^>]*>)?"
    r"(?:\s+extends\s+([\w.]+)(?:\s*<[^>]*>)?)?"
    r"(?:\s+implements\s+([^{]+))?",
    re.M,
)
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?((?:api|ui|common|base)\.[\w.*]+)\s*;", re.M)


def layer_of(pkg, simple_name=""):
    """Map a package (test packages are rooted at api./ui.) to a layer name."""
    for layer, prefixes in LAYERS:
        for p in prefixes:
            if pkg == p or pkg.startswith(p + ".") or simple_name == p.split(".")[-1]:
                return layer
    return "Other"


class Type:
    def __init__(self, name, kind, pkg, path, extends, implements):
        self.name, self.kind, self.pkg, self.path = name, kind, pkg, path
        self.extends, self.implements = extends, implements
        self.is_test = "src/test/" in str(path)
        self.layer = "Test" if self.is_test else layer_of(pkg, name)
        self.fqn = f"{pkg}.{name}"


def parse():
    types, edges = {}, defaultdict(int)          # edges: (from_pkg, to_pkg) -> count
    class_edges = defaultdict(set)               # fqn -> set of imported fqns
    for path in sorted(SRC.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        pm = PACKAGE_RE.search(text)
        if not pm:
            continue
        pkg = pm.group(1)
        imports = IMPORT_RE.findall(text)

        # only the file's top-level (least-indented) declaration
        decls = TYPE_RE.findall(text)
        if not decls:
            continue
        kind, name, ext, impl = decls[0]
        t = Type(name, kind, pkg, path,
                 ext.split(".")[-1] if ext else None,
                 [i.strip().split("<")[0].split(".")[-1] for i in impl.split(",")] if impl else [])
        types[t.fqn] = t

        for imp in imports:
            to_pkg = re.sub(r"\.(\*|[A-Z][\w]*)$", "", imp)
            if to_pkg != pkg:
                edges[(pkg, to_pkg)] += 1
            class_edges[t.fqn].add(imp)
    return types, edges, class_edges


def group_by_layer(types):
    by = defaultdict(list)
    for t in types.values():
        by[t.layer].append(t)
    return by


# ---------------------------------------------------------------- diagram 1
def write_packages(edges, types):
    pkg_layer = {t.pkg: t.layer for t in types.values()}
    layer_edges = defaultdict(int)
    for (a, b), n in edges.items():
        la, lb = pkg_layer.get(a, "Other"), pkg_layer.get(b, "Other")
        if la != lb:
            layer_edges[(la, lb)] += n

    lines = ["@startuml", "title TeamCity Test Framework - Layer Dependencies",
             "skinparam componentStyle rectangle", "left to right direction", ""]
    for layer, _ in LAYERS:
        lines.append(f'component "{layer}" as {layer}')
    lines.append("")
    for (a, b), n in sorted(layer_edges.items(), key=lambda kv: -kv[1]):
        if a in dict(LAYERS) and b in dict(LAYERS):
            style = "-[#red,bold]->" if _is_upward(a, b) else "-->"
            lines.append(f"{a} {style} {b} : {n}")
    lines += ["", "legend right", "  Red = dependency pointing UP the stack (layering violation)",
              "  Number = count of import statements", "endlegend", "@enduml"]
    (OUT / "01-packages.puml").write_text("\n".join(lines))


ORDER = [l for l, _ in LAYERS]


def _is_upward(a, b):
    """True when a lower layer depends on a higher one."""
    try:
        return ORDER.index(b) < ORDER.index(a)
    except ValueError:
        return False


# ---------------------------------------------------------------- diagram 2
def write_classes(types):
    by = group_by_layer(types)
    lines = ["@startuml", "title TeamCity Test Framework - All Classes (inheritance)",
             "skinparam classAttributeIconSize 0", "skinparam linetype ortho",
             "hide members", "hide circle", ""]
    for layer in ORDER + ["Other"]:
        if layer not in by:
            continue
        lines.append(f'package "{layer}" {{')
        for t in sorted(by[layer], key=lambda x: x.name):
            kw = {"@interface": "annotation", "interface": "interface",
                  "enum": "enum", "class": "class"}[t.kind]
            lines.append(f"  {kw} {t.name}")
        lines.append("}")
        lines.append("")

    names = {t.name for t in types.values()}
    for t in sorted(types.values(), key=lambda x: x.name):
        if t.extends and t.extends in names:
            lines.append(f"{t.extends} <|-- {t.name}")
        for i in t.implements:
            if i in names:
                lines.append(f"{i} <|.. {t.name}")
    lines.append("@enduml")
    (OUT / "02-classes.puml").write_text("\n".join(lines))


# ---------------------------------------------------------------- diagram 3
def write_layer_details(types, class_edges):
    by = group_by_layer(types)
    simple = {}
    for t in types.values():
        simple.setdefault(t.name, t)
    for layer, members in by.items():
        if layer == "Other":
            continue
        lines = ["@startuml", f"title {layer} layer", "hide empty members", ""]
        member_names = {t.name for t in members}
        for t in sorted(members, key=lambda x: x.name):
            kw = {"@interface": "annotation", "interface": "interface",
                  "enum": "enum", "class": "class"}[t.kind]
            lines.append(f"{kw} {t.name}")
        lines.append("")
        external = set()
        for t in members:
            if t.extends and t.extends not in member_names and t.extends in simple:
                external.add(t.extends)
            for imp in class_edges.get(t.fqn, ()):
                target = imp.split(".")[-1]
                if target in simple and target not in member_names:
                    external.add(target)
        for e in sorted(external):
            lines.append(f'class {e} #LightGray ##[dashed] {{\n  .. {simple[e].layer} ..\n}}')
        lines.append("")
        for t in sorted(members, key=lambda x: x.name):
            if t.extends and t.extends in simple:
                lines.append(f"{t.extends} <|-- {t.name}")
            for i in t.implements:
                if i in simple:
                    lines.append(f"{i} <|.. {t.name}")
            for imp in sorted(class_edges.get(t.fqn, ())):
                target = imp.split(".")[-1]
                if target in simple and target != t.name and target != t.extends:
                    lines.append(f"{t.name} ..> {target}")
        lines.append("@enduml")
        (OUT / f"03-{layer.lower()}.puml").write_text("\n".join(lines))


# ---------------------------------------------------------------- metrics
def write_coupling(types, edges):
    pkg_layer = {t.pkg: t.layer for t in types.values()}
    ce, ca = defaultdict(int), defaultdict(int)
    pair = defaultdict(int)
    for (a, b), n in edges.items():
        la, lb = pkg_layer.get(a, "Other"), pkg_layer.get(b, "Other")
        if la == lb:
            continue
        ce[la] += n
        ca[lb] += n
        pair[(la, lb)] += n

    rows = []
    for layer in ORDER + ["Other"]:
        e, a = ce.get(layer, 0), ca.get(layer, 0)
        if e == a == 0:
            continue
        inst = e / (e + a) if (e + a) else 0.0
        rows.append((layer, e, a, inst))

    md = ["# Coupling metrics (generated)", "",
          "Ce = outgoing imports, Ca = incoming, I = Ce/(Ce+Ca).",
          "I near 0 = stable (safe to depend on); I near 1 = volatile.",
          "Healthy layering: I decreases as you go down the stack.", "",
          "| Layer | Ce | Ca | I |", "| --- | ---: | ---: | ---: |"]
    for layer, e, a, i in sorted(rows, key=lambda r: -r[3]):
        md.append(f"| {layer} | {e} | {a} | {i:.2f} |")

    md += ["", "## Cycles between layers", "",
           "| A | B | A->B | B->A |", "| --- | --- | ---: | ---: |"]
    seen = set()
    for (a, b), n in sorted(pair.items()):
        if (b, a) in pair and (b, a) not in seen:
            seen.add((a, b))
            md.append(f"| {a} | {b} | {n} | {pair[(b, a)]} |")

    md += ["", "## Upward dependencies (lower layer importing a higher one)", "",
           "| From | To | Imports |", "| --- | --- | ---: |"]
    for (a, b), n in sorted(pair.items(), key=lambda kv: -kv[1]):
        if _is_upward(a, b):
            md.append(f"| {a} | {b} | {n} |")

    (OUT / "coupling.md").write_text("\n".join(md) + "\n")


def main():
    OUT.mkdir(exist_ok=True)
    types, edges, class_edges = parse()
    write_packages(edges, types)
    write_classes(types)
    write_layer_details(types, class_edges)
    write_coupling(types, edges)
    print(f"Parsed {len(types)} types, {sum(edges.values())} intra-project imports.")
    for f in sorted(OUT.iterdir()):
        print(f"  {f.relative_to(ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
