#!/usr/bin/env python3
"""
UI route / page-object coverage for the TeamCity Selenide suite.

There is no swagger.json for a UI, so the denominator has to come from the code.
This script derives it statically:

  denominator  every concrete page object under src/main/java/ui/pages
               (plus the distinct URL templates their url() methods declare)
  numerator    which of those page objects the tests under src/test/java actually
               reach, split into two grades:
                 opened   -- the test navigates to it by URL  (new X().open(...))
                 reached  -- the test lands on it some other way: getPage(X.class),
                             or by calling a page-object method whose return type
                             is X (a navigation transition)

Java comments are stripped before any scanning, so commented-out drafts (e.g.
nk_Test_Ideas.java) contribute nothing and references to classes that no longer
exist do not inflate the numbers.

Outputs a standalone HTML report and a JSON summary for the quality dashboard.

Usage:
    python3 scripts/build_route_coverage.py \
        --out my_reports/ui-route-coverage.html \
        --json my_reports/ui-route-coverage.json
"""

import argparse
import html
import json
import os
import re
import sys
from collections import defaultdict
from datetime import datetime, timezone

# --------------------------------------------------------------------------------------
# Java source parsing
# --------------------------------------------------------------------------------------

CLASS_RE = re.compile(r"\b(?:public\s+)?(abstract\s+)?class\s+(\w+)(?:\s*<[^>]*>)?\s*(?:extends\s+(\w+))?")
URL_RE = re.compile(r"\bString\s+url\s*\(\s*\)\s*\{\s*return\s+(\"(?:[^\"\\]|\\.)*\")\s*;")
METHOD_RE = re.compile(r"\b(?:public|protected)\s+(?:static\s+)?(?:final\s+)?([A-Z]\w*)\s+(\w+)\s*\(")
TEST_ANNOTATION_RE = re.compile(r"@Test\b")
DISPLAY_NAME_RE = re.compile(r'@DisplayName\s*\(\s*"((?:[^"\\]|\\.)*)"')


def strip_comments(src: str) -> str:
    """Remove // and /* */ comments while respecting string/char literals.

    Newlines inside block comments are preserved so line numbers stay meaningful.
    """
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                if src[i] == "\\":
                    out.append(src[i:i + 2])
                    i += 2
                    continue
                out.append(src[i])
                if src[i] == '"':
                    i += 1
                    break
                i += 1
            continue
        if c == "'":
            out.append(c)
            i += 1
            while i < n:
                if src[i] == "\\":
                    out.append(src[i:i + 2])
                    i += 2
                    continue
                out.append(src[i])
                if src[i] == "'":
                    i += 1
                    break
                i += 1
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            while i < n and src[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "*":
            i += 2
            while i + 1 < n and not (src[i] == "*" and src[i + 1] == "/"):
                if src[i] == "\n":
                    out.append("\n")
                i += 1
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def java_files(root):
    for dirpath, _dirnames, filenames in os.walk(root):
        for fn in sorted(filenames):
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def read(path):
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        return fh.read()


# --------------------------------------------------------------------------------------
# Page inventory
# --------------------------------------------------------------------------------------

class Page:
    def __init__(self, name, path, parent, abstract, own_url):
        self.name = name
        self.path = path
        self.parent = parent
        self.abstract = abstract
        self.own_url = own_url          # url() declared on this class, or None
        self.url = None                 # resolved through the extends chain
        self.url_inherited_from = None
        self.transitions = {}           # method name -> this page returns that page
        self.subclassed_by = []         # page classes that extend this one
        self.opened_by = set()          # test ids that open it by URL
        self.reached_by = set()         # test ids that land on it some other way

    @property
    def status(self):
        if self.opened_by:
            return "opened"
        if self.reached_by:
            return "reached"
        return "untouched"

    @property
    def touched_by(self):
        return self.opened_by | self.reached_by


def collect_pages(pages_dir):
    pages = {}
    for path in java_files(pages_dir):
        src = strip_comments(read(path))
        m = CLASS_RE.search(src)
        if not m:
            continue
        abstract, name, parent = bool(m.group(1)), m.group(2), m.group(3)
        um = URL_RE.search(src)
        own_url = json.loads(um.group(1)) if um else None
        pages[name] = Page(name, path, parent, abstract, own_url)

    # Resolve url() through the extends chain.
    for page in pages.values():
        node, seen = page, set()
        while node is not None and node.name not in seen:
            seen.add(node.name)
            if node.own_url is not None:
                page.url = node.own_url
                page.url_inherited_from = None if node is page else node.name
                break
            node = pages.get(node.parent)

    for page in pages.values():
        if page.parent in pages:
            pages[page.parent].subclassed_by.append(page.name)
    return pages


def collect_transitions(pages):
    """Map page-object method name -> set of page classes that method navigates to.

    A method whose return type is its own declaring class is a fluent self-return
    (`return this`), not a navigation, so it is excluded — otherwise a name shared
    with a real transition elsewhere (e.g. runBuild) credits the wrong page.
    """
    transitions = defaultdict(set)
    for page in pages.values():
        src = strip_comments(read(page.path))
        for ret, method in METHOD_RE.findall(src):
            if ret == page.name:
                continue
            if ret in pages and not pages[ret].abstract:
                transitions[method].add(ret)
                page.transitions[method] = ret
    return transitions


# --------------------------------------------------------------------------------------
# Test scanning
# --------------------------------------------------------------------------------------

class TestFile:
    def __init__(self, path, rel, test_count, is_test):
        self.path = path
        self.rel = rel
        self.test_count = test_count
        self.is_test = is_test          # has at least one @Test
        self.pages = set()
        self.opened = set()

    @property
    def is_ui_test(self):
        """A UI test drives at least one page object, or lives in the UI test tree.

        The scan covers the whole test root so shared base classes contribute
        reachability, but API tests must not land in the UI denominator.
        """
        in_ui_tree = f"{os.sep}ui{os.sep}" in self.rel or self.rel.startswith(f"ui{os.sep}")
        return self.is_test and (bool(self.pages) or in_ui_tree)


def scan_tests(test_root, pages, transitions):
    files = []
    page_names = set(pages)
    concrete = {n for n, p in pages.items() if not p.abstract}

    for path in java_files(test_root):
        src = strip_comments(read(path))
        rel = os.path.relpath(path)
        tf = TestFile(path, rel, len(TEST_ANNOTATION_RE.findall(src)), bool(TEST_ANNOTATION_RE.search(src)))

        # Direct construction, optionally followed by .open(...) somewhere in the chain.
        for name in concrete:
            for m in re.finditer(r"\bnew\s+" + name + r"\s*\(\s*\)", src):
                tf.pages.add(name)
                # .open( may be on the next line of a fluent chain
                tail = src[m.end():m.end() + 200]
                if re.match(r"\s*\.\s*open\s*\(", tail):
                    tf.opened.add(name)

        # getPage(X.class) / page(X.class) / open(X.class)
        for m in re.finditer(r"\b(?:getPage|page|open)\s*\(\s*(\w+)\s*\.class", src):
            if m.group(1) in concrete:
                tf.pages.add(m.group(1))

        # Navigation transitions: a call to a page-object method that returns a page.
        for m in re.finditer(r"\.\s*(\w+)\s*\(", src):
            targets = transitions.get(m.group(1))
            if targets:
                tf.pages.update(targets)

        in_ui_tree = f"{os.sep}ui{os.sep}" in rel
        if tf.pages or tf.is_test or in_ui_tree:
            files.append(tf)

    # Files without @Test (base classes, helpers) still establish reachability;
    # attribute their references to every test file so pages are not counted as
    # untouched when a shared base class is what navigates there.
    shared = set()
    for tf in files:
        if not tf.is_test:
            shared |= tf.pages

    for tf in files:
        if not tf.is_test:
            continue
        for name in tf.opened:
            pages[name].opened_by.add(tf.rel)
        for name in tf.pages - tf.opened:
            pages[name].reached_by.add(tf.rel)

    for name in shared:
        if name in pages and not pages[name].touched_by:
            pages[name].reached_by.add("(shared setup)")

    return files, page_names


# --------------------------------------------------------------------------------------
# Rendering helpers
# --------------------------------------------------------------------------------------

def e(text) -> str:
    return html.escape(str(text), quote=True)


def pct(num, den):
    return 0.0 if not den else 100.0 * num / den


def fmt_pct(value, digits=1):
    return f"{value:.{digits}f}%"


CSS = """
*,*::before,*::after{box-sizing:border-box}
:root{
  color-scheme:light;
  --surface-0:#f4f3f0; --surface-1:#fcfcfb; --border:#e2e0da;
  --text-primary:#0b0b0b; --text-secondary:#52514e; --text-muted:#83817a;
  --accent:#2a78d6; --deemph:#c9c7c0;
  --cov-full:#1c5cab; --cov-partial:#3987e5; --cov-empty:#86b6ef;
  --good:#0ca30c; --warn:#fab219; --bad:#d03b3b;
  --good-ink:#0a7a0a; --warn-ink:#8a6100; --bad-ink:#b32f2f;
}
@media (prefers-color-scheme:dark){
  :root:not([data-theme="light"]){
    color-scheme:dark;
    --surface-0:#121211; --surface-1:#1a1a19; --border:#333330;
    --text-primary:#ffffff; --text-secondary:#c3c2b7; --text-muted:#96958c;
    --accent:#3987e5; --deemph:#4a4a46;
    --cov-full:#184f95; --cov-partial:#2a78d6; --cov-empty:#6da7ec;
    --good-ink:#48c048; --warn-ink:#fab219; --bad-ink:#e66767;
  }
}
:root[data-theme="dark"]{
  color-scheme:dark;
  --surface-0:#121211; --surface-1:#1a1a19; --border:#333330;
  --text-primary:#ffffff; --text-secondary:#c3c2b7; --text-muted:#96958c;
  --accent:#3987e5; --deemph:#4a4a46;
  --cov-full:#184f95; --cov-partial:#2a78d6; --cov-empty:#6da7ec;
  --good-ink:#48c048; --warn-ink:#fab219; --bad-ink:#e66767;
}
html,body{margin:0;padding:0}
body{
  background:var(--surface-0); color:var(--text-primary);
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
  font-size:15px; line-height:1.5; overflow-x:hidden;
}
.wrap{max-width:1180px;margin:0 auto;padding:28px 20px 64px}
a{color:var(--accent)}
h1{font-size:26px;line-height:1.2;margin:0 0 4px;font-weight:650;letter-spacing:-.01em;text-wrap:balance}
h2{font-size:18px;margin:0 0 4px;font-weight:620;letter-spacing:-.005em;text-wrap:balance}
.sub{color:var(--text-secondary);font-size:13.5px;margin:0;max-width:74ch}
.muted{color:var(--text-muted)}
p.note{font-size:13.5px;color:var(--text-secondary);margin:10px 0 0;max-width:74ch}
header.page{display:flex;flex-wrap:wrap;gap:16px;align-items:flex-start;justify-content:space-between;margin-bottom:22px}
.btn{
  display:inline-flex;align-items:center;gap:6px;padding:6px 12px;border-radius:8px;
  border:1px solid var(--border);background:var(--surface-1);color:var(--text-primary);
  font-size:13px;text-decoration:none;cursor:pointer;font-family:inherit
}
.btn:hover{border-color:var(--accent);color:var(--accent)}
.btn:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
.card{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:18px 20px;margin-bottom:18px}
.chart{width:100%;height:auto;display:block;overflow:visible;margin-top:12px}
.scroll{overflow-x:auto;max-width:100%}
.grid-kpi{display:grid;grid-template-columns:repeat(auto-fit,minmax(178px,1fr));gap:12px;margin-bottom:18px}
.tile{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:14px 16px;min-width:0}
.tile .label{font-size:12.5px;color:var(--text-secondary);margin-bottom:6px}
.tile .value{font-size:27px;font-weight:640;letter-spacing:-.02em;line-height:1.1;overflow-wrap:anywhere}
.tile .note{font-size:12px;color:var(--text-muted);margin-top:5px}
.hero{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:22px 24px;margin-bottom:18px;
  display:flex;flex-wrap:wrap;gap:22px;align-items:center;justify-content:space-between}
.hero .figure{font-size:56px;font-weight:660;letter-spacing:-.03em;line-height:1}
.hero .caption{font-size:13.5px;color:var(--text-secondary);margin-top:6px;max-width:56ch}
.hero .aside{display:flex;gap:26px;flex-wrap:wrap}
.hero .aside div{min-width:88px}
.hero .aside .v{font-size:20px;font-weight:620;letter-spacing:-.01em;font-variant-numeric:tabular-nums}
.hero .aside .k{font-size:12px;color:var(--text-secondary)}
.legend{display:flex;flex-wrap:wrap;gap:14px;margin-top:12px;font-size:12.5px;color:var(--text-secondary)}
.legend span{display:inline-flex;align-items:center;gap:6px}
.swatch{width:11px;height:11px;border-radius:3px;display:inline-block;flex:none}
table{border-collapse:collapse;width:100%;font-size:13px}
th,td{text-align:left;padding:7px 10px;border-bottom:1px solid var(--border);white-space:nowrap}
th{font-weight:600;color:var(--text-secondary);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
td.num,th.num{text-align:right;font-variant-numeric:tabular-nums}
tbody tr:hover{background:var(--surface-0)}
td.wrapname{white-space:normal;min-width:200px;overflow-wrap:anywhere}
code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12.5px}
.pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;padding:2px 8px;border-radius:99px;border:1px solid var(--border)}
.pill.good{color:var(--good-ink)} .pill.warn{color:var(--warn-ink)} .pill.bad{color:var(--bad-ink)} .pill.none{color:var(--text-muted)}
.callout{border:1px solid var(--border);border-left:3px solid var(--warn);border-radius:8px;
  background:var(--surface-0);padding:13px 16px;margin-top:14px;font-size:13.5px;color:var(--text-secondary);max-width:82ch}
.callout strong{color:var(--text-primary)}
.rowlabel{fill:var(--text-secondary);font-size:12.5px}
.barval{fill:var(--text-primary);font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}
.mark{cursor:pointer}
.mark:hover{opacity:.82}
.empty{color:var(--text-muted);font-size:13px;margin:10px 0 0}
details{border-top:1px solid var(--border);padding:9px 0}
details summary{cursor:pointer;font-size:13.5px;color:var(--text-secondary);list-style-position:outside}
details summary:hover{color:var(--accent)}
details summary strong{color:var(--text-primary)}
details[open] summary{margin-bottom:8px}
details table td{vertical-align:top}
details .pill{margin-right:4px}
#tip{
  position:fixed;pointer-events:none;opacity:0;transition:opacity .1s;z-index:50;
  background:var(--text-primary);color:var(--surface-1);font-size:12.5px;
  padding:5px 9px;border-radius:6px;max-width:340px;line-height:1.35
}
footer{margin-top:30px;font-size:12.5px;color:var(--text-muted);line-height:1.7;max-width:82ch}
@media (prefers-reduced-motion:reduce){*{transition:none!important;animation:none!important}}
@media (max-width:640px){.hero .figure{font-size:44px}.wrap{padding:18px 14px 48px}}
"""

JS = """
(function(){
  var root=document.documentElement, tip=document.getElementById('tip');
  var stored=null; try{stored=localStorage.getItem('qr-theme');}catch(e){}
  if(stored){root.setAttribute('data-theme',stored);}
  var btn=document.getElementById('theme');
  if(btn){btn.addEventListener('click',function(){
    var dark=getComputedStyle(root).getPropertyValue('--surface-1').trim()==='#1a1a19';
    var next=dark?'light':'dark';
    root.setAttribute('data-theme',next);
    try{localStorage.setItem('qr-theme',next);}catch(e){}
  });}
  function show(el,x,y){
    tip.textContent=el.getAttribute('data-tip');
    tip.style.opacity='1';
    var r=tip.getBoundingClientRect();
    var left=Math.min(Math.max(8,x+12),window.innerWidth-r.width-8);
    var top=Math.max(8,y-r.height-10);
    tip.style.left=left+'px'; tip.style.top=top+'px';
  }
  document.addEventListener('mousemove',function(ev){
    var el=ev.target.closest?ev.target.closest('[data-tip]'):null;
    if(el){show(el,ev.clientX,ev.clientY);} else {tip.style.opacity='0';}
  });
})();
"""

STATUS_FILL = {"opened": "var(--cov-full)", "reached": "var(--cov-partial)", "untouched": "var(--deemph)"}
STATUS_PILL = {"opened": "good", "reached": "warn", "untouched": "none"}
STATUS_LABEL = {"opened": "opened by URL", "reached": "reached by navigation", "untouched": "never touched"}


def tile(label, value, note=""):
    note_html = f'<div class="note">{e(note)}</div>' if note else ""
    return (f'<div class="tile"><div class="label">{e(label)}</div>'
            f'<div class="value">{e(value)}</div>{note_html}</div>')


def svg_page_bars(rows):
    """One row per page object: a bar whose width is the number of tests touching it."""
    row_h, gap, label_w, pad_r = 22, 5, 210, 60
    width, height = 1000, len(rows) * (row_h + gap) + 8
    plot_w = width - label_w - pad_r
    max_n = max([r["tests"] for r in rows] + [1])
    parts = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
             f'aria-label="Tests touching each page object">']
    for i, r in enumerate(rows):
        y = i * (row_h + gap)
        bar_w = max(3, plot_w * r["tests"] / max_n) if r["tests"] else 3
        fill = STATUS_FILL[r["status"]]
        tip = (f'{r["name"]} — {STATUS_LABEL[r["status"]]}; '
               f'{r["tests"]} test file(s); route: {r["url"] or "none declared"}')
        parts.append(
            f'<text class="rowlabel" x="{label_w - 10}" y="{y + row_h / 2 + 4}" text-anchor="end">{e(r["name"])}</text>'
            f'<rect class="mark" x="{label_w}" y="{y}" width="{bar_w:.1f}" height="{row_h}" rx="4" '
            f'fill="{fill}" data-tip="{e(tip)}"></rect>'
            f'<text class="barval" x="{label_w + bar_w + 8:.1f}" y="{y + row_h / 2 + 4}">{r["tests"]}</text>'
        )
    parts.append("</svg>")
    return "".join(parts)


def load_inventory(path):
    """The declared product inventory: what pages TeamCity has, not what we modeled."""
    if not path or not os.path.isfile(path):
        return None
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def inventory_stats(inventory, pages):
    """Grade every declared product page: tested > modeled > missing.

    'modeled' means a page object exists but no test reaches it -- a real state,
    and one the raw page-object count hides.
    """
    areas, unknown_refs = [], []
    for area in inventory["areas"]:
        rows = []
        for p in area["pages"]:
            ref = p.get("modeled_by")
            page = pages.get(ref) if ref else None
            if ref and page is None:
                unknown_refs.append((area["name"], p["name"], ref))
            if page is not None and page.status != "untouched":
                grade = "tested"
            elif page is not None:
                grade = "modeled"
            else:
                grade = "missing"
            rows.append({
                "name": p["name"],
                "route": p.get("route") or area.get("route_template"),
                "modeled_by": ref,
                "grade": grade,
                "conditional": p.get("conditional"),
                "tab_id": p.get("tab_id"),
            })
        areas.append({
            "name": area["name"],
            "source": area.get("source", "docs"),
            "note": area.get("note", ""),
            "rows": rows,
            "total": len(rows),
            "tested": sum(1 for r in rows if r["grade"] == "tested"),
            "modeled": sum(1 for r in rows if r["grade"] != "missing"),
            "missing": sum(1 for r in rows if r["grade"] == "missing"),
            "conditional": sum(1 for r in rows if r["conditional"]),
        })

    totals = {
        "total": sum(a["total"] for a in areas),
        "tested": sum(a["tested"] for a in areas),
        "modeled": sum(a["modeled"] for a in areas),
        "missing": sum(a["missing"] for a in areas),
        "conditional": sum(a["conditional"] for a in areas),
    }
    mapped = {r["modeled_by"] for a in areas for r in a["rows"] if r["modeled_by"]}
    unmapped = sorted(n for n, p in pages.items()
                      if not p.abstract and n not in mapped and not p.subclassed_by)
    return areas, totals, unknown_refs, unmapped


GRADE_FILL = {"tested": "var(--cov-full)", "modeled": "var(--cov-empty)", "missing": "var(--deemph)"}
GRADE_PILL = {"tested": "good", "modeled": "warn", "missing": "none"}
GRADE_LABEL = {"tested": "tested", "modeled": "modeled, untested", "missing": "not modeled"}


def svg_area_bars(areas, totals):
    """One row per product area; segment widths are page counts on a shared scale."""
    row_h, gap, label_w, pad_r = 26, 7, 250, 76
    width, height = 1000, len(areas) * (row_h + gap) + 8
    plot_w = width - label_w - pad_r
    scale = plot_w / max(totals["total"] and max(a["total"] for a in areas) or 1, 1)
    parts = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
             f'aria-label="Product page coverage by area">']
    for i, a in enumerate(areas):
        y = i * (row_h + gap)
        x = label_w
        segs = [("tested", a["tested"]), ("modeled", a["modeled"] - a["tested"]), ("missing", a["missing"])]
        parts.append(f'<text class="rowlabel" x="{label_w - 10}" y="{y + row_h / 2 + 4}" '
                     f'text-anchor="end">{e(a["name"])}</text>')
        for grade, n in segs:
            if not n:
                continue
            w = n * scale
            tip = f'{a["name"]} — {n} {GRADE_LABEL[grade]} of {a["total"]}'
            parts.append(f'<rect class="mark" x="{x:.1f}" y="{y}" width="{w:.1f}" height="{row_h}" '
                         f'fill="{GRADE_FILL[grade]}" data-tip="{e(tip)}"></rect>')
            x += w
        parts.append(f'<text class="barval" x="{x + 8:.1f}" y="{y + row_h / 2 + 4}">'
                     f'{a["tested"]}/{a["total"]}</text>')
    parts.append("</svg>")
    return "".join(parts)


def product_section(inventory, pages):
    areas, totals, unknown_refs, unmapped = inventory_stats(inventory, pages)

    tiles = "".join([
        tile("Pages TeamCity has", f"{totals['total']}", f"declared for {inventory.get('server', 'this server')}"),
        tile("Covered by a test", f"{totals['tested']}", f"{fmt_pct(pct(totals['tested'], totals['total']))} of the product"),
        tile("Modeled but untested", f"{totals['modeled'] - totals['tested']}", "page object exists, no test reaches it"),
        tile("Not modeled at all", f"{totals['missing']}", "no page object exists"),
    ])

    area_rows = "".join(
        f'<tr><td class="wrapname"><strong>{e(a["name"])}</strong></td>'
        f'<td class="num">{a["total"]}</td><td class="num">{a["tested"]}</td>'
        f'<td class="num">{a["modeled"] - a["tested"]}</td><td class="num">{a["missing"]}</td>'
        f'<td class="num">{fmt_pct(pct(a["tested"], a["total"]), 0)}</td>'
        f'<td><span class="pill {"good" if a["source"] == "measured" else "none"}">{e(a["source"])}</span></td></tr>'
        for a in areas
    )
    area_rows += (
        f'<tr><td class="wrapname"><strong>Total</strong></td>'
        f'<td class="num"><strong>{totals["total"]}</strong></td>'
        f'<td class="num"><strong>{totals["tested"]}</strong></td>'
        f'<td class="num"><strong>{totals["modeled"] - totals["tested"]}</strong></td>'
        f'<td class="num"><strong>{totals["missing"]}</strong></td>'
        f'<td class="num"><strong>{fmt_pct(pct(totals["tested"], totals["total"]), 0)}</strong></td><td></td></tr>')

    details = []
    for a in areas:
        items = []
        for r in a["rows"]:
            bits = [f'<span class="pill {GRADE_PILL[r["grade"]]}">{e(GRADE_LABEL[r["grade"]])}</span>']
            if r["modeled_by"]:
                bits.append(f'<code>{e(r["modeled_by"])}</code>')
            if r["route"]:
                bits.append(f'<code>{e(r["route"])}</code>')
            if r["conditional"]:
                bits.append(f'<span class="muted">conditional — {e(r["conditional"])}</span>')
            items.append(f'<tr><td class="wrapname">{e(r["name"])}</td><td class="wrapname">{" ".join(bits)}</td></tr>')
        note = f'<p class="note">{e(a["note"])}</p>' if a["note"] else ""
        details.append(
            f'<details><summary><strong>{e(a["name"])}</strong> — '
            f'{a["tested"]} of {a["total"]} covered</summary>{note}'
            f'<div class="scroll"><table><tbody>{"".join(items)}</tbody></table></div></details>')

    warnings = []
    if unknown_refs:
        warnings.append(
            '<div class="callout"><strong>Inventory references a page object that does not exist.</strong> '
            'Either the class was never written or it was renamed — the inventory is stale.<ul>' +
            "".join(f'<li>{e(area)} → {e(page)} expects <code>{e(ref)}</code></li>'
                    for area, page, ref in unknown_refs) + "</ul></div>")
    if unmapped:
        warnings.append(
            '<div class="callout"><strong>Page objects not mapped to any declared page.</strong> '
            'These exist in the suite but no inventory entry claims them, so they are absent from the '
            'product numbers above. Either add them to the inventory or delete them.<ul>' +
            "".join(f"<li><code>{e(n)}</code></li>" for n in unmapped) + "</ul></div>")

    return f"""
<div class="card">
  <h2>Product coverage — the whole of TeamCity</h2>
  <p class="sub">The denominator here is <strong>every page TeamCity has</strong>
  ({totals['total']} declared in <code>ui-page-inventory.json</code>), not just the pages this suite
  models. This is the number that answers "how much is left to do"; everything else in this report
  measures the suite against itself.</p>
  <div class="grid-kpi" style="margin-top:16px">{tiles}</div>
  <div class="scroll">{svg_area_bars(areas, totals)}</div>
  <div class="legend">
    <span><i class="swatch" style="background:{GRADE_FILL['tested']}"></i>tested</span>
    <span><i class="swatch" style="background:{GRADE_FILL['modeled']}"></i>modeled, untested</span>
    <span><i class="swatch" style="background:{GRADE_FILL['missing']}"></i>not modeled</span>
  </div>
  <div class="scroll" style="margin-top:16px"><table>
    <thead><tr><th>Area</th><th class="num">Pages</th><th class="num">Tested</th>
    <th class="num">Modeled only</th><th class="num">Missing</th><th class="num">Covered</th><th>Count from</th></tr></thead>
    <tbody>{area_rows}</tbody>
  </table></div>
  <p class="note">{totals['conditional']} of the {totals['total']} pages are conditional — Root-project only,
  hidden by default, or shown only when a feature is enabled — so the practically reachable denominator is
  nearer {totals['total'] - totals['conditional']}.</p>
  <h3 style="margin-top:18px">Every declared page</h3>
  {"".join(details)}
  {"".join(warnings)}
</div>
"""


def render(pages, files, args, ambiguous, inventory):
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    concrete = sorted([p for p in pages.values() if not p.abstract], key=lambda p: p.name)

    opened = [p for p in concrete if p.status == "opened"]
    reached = [p for p in concrete if p.status == "reached"]
    untouched = [p for p in concrete if p.status == "untouched"]
    touched = opened + reached

    routed = [p for p in concrete if p.url]
    routes = sorted({p.url for p in routed})
    routes_hit = sorted({p.url for p in routed if p.status != "untouched"})
    no_route = [p for p in concrete if not p.url]

    tests = [f for f in files if f.is_ui_test]
    test_cases = sum(f.test_count for f in tests)

    page_cov = pct(len(touched), len(concrete))
    route_cov = pct(len(routes_hit), len(routes))

    rows = [{"name": p.name, "status": p.status, "tests": len(p.touched_by), "url": p.url} for p in concrete]
    rows.sort(key=lambda r: (-r["tests"], r["name"]))

    kpis = "".join([
        tile("Page objects", f"{len(concrete)}", "concrete classes under ui/pages"),
        tile("Opened by URL", f"{len(opened)}", f"{fmt_pct(pct(len(opened), len(concrete)))} of page objects"),
        tile("Reached by navigation", f"{len(reached)}", "landed on without a direct open()"),
        tile("Never touched", f"{len(untouched)}", "no test path reaches these"),
        tile("Distinct routes", f"{len(routes_hit)}/{len(routes)}", "URL templates declared by url()"),
        tile("Test cases scanned", f"{test_cases}", f"across {len(tests)} test classes"),
    ])

    inv_rows = []
    for p in concrete:
        route = f"<code>{e(p.url)}</code>" if p.url else '<span class="muted">— navigation-only</span>'
        if p.url_inherited_from:
            route += f' <span class="muted">(from {e(p.url_inherited_from)})</span>'
        by = sorted(os.path.basename(t) for t in p.touched_by)
        by_html = ", ".join(e(b) for b in by) if by else '<span class="muted">—</span>'
        inv_rows.append(
            f'<tr><td class="wrapname"><strong>{e(p.name)}</strong></td>'
            f'<td>{route}</td>'
            f'<td><span class="pill {STATUS_PILL[p.status]}">{e(STATUS_LABEL[p.status])}</span></td>'
            f'<td class="num">{len(p.touched_by)}</td>'
            f'<td class="wrapname">{by_html}</td></tr>'
        )

    test_rows = []
    for f in sorted(tests, key=lambda f: f.rel):
        names = ", ".join(e(n) for n in sorted(f.pages)) if f.pages else '<span class="muted">none</span>'
        test_rows.append(
            f'<tr><td class="wrapname"><code>{e(f.rel)}</code></td>'
            f'<td class="num">{f.test_count}</td>'
            f'<td class="num">{len(f.pages)}</td>'
            f'<td class="wrapname">{names}</td></tr>'
        )

    empty_test_files = [f for f in files if not f.is_test
                        and f"{os.sep}ui{os.sep}" in f.rel]

    real_gaps = [p for p in untouched if not p.subclassed_by]
    base_only = [p for p in untouched if p.subclassed_by]

    gaps = []
    if real_gaps:
        gaps.append("<p class=\"note\"><strong>Page objects no test reaches.</strong> Each is either an untested "
                    "area of the product or dead code that should be deleted.</p><ul>" +
                    "".join(f"<li><code>{e(p.name)}</code>"
                            + (f" — declares <code>{e(p.url)}</code>" if p.url else " — no route declared")
                            + "</li>" for p in real_gaps) + "</ul>")
    if base_only:
        gaps.append("<p class=\"note\"><strong>Never used directly, but subclassed.</strong> These behave as base "
                    "classes: no test names them, yet their route and behaviour are exercised through a subclass. "
                    "Not a coverage gap — but if a class is only ever a base, it belongs in the hierarchy as "
                    "<code>abstract</code>, which would take it out of this denominator.</p><ul>" +
                    "".join(f"<li><code>{e(p.name)}</code> — extended by "
                            + ", ".join(f"<code>{e(s)}</code>" for s in sorted(p.subclassed_by)) + "</li>"
                            for p in base_only) + "</ul>")
    if no_route:
        gaps.append("<p class=\"note\"><strong>Page objects with an empty <code>url()</code>.</strong> "
                    "These cannot be opened directly, so they are only ever reachable through another page. "
                    "Giving them a real route makes them independently testable and shrinks setup time.</p><ul>" +
                    "".join(f"<li><code>{e(p.name)}</code></li>" for p in no_route) + "</ul>")
    gaps_html = "".join(gaps) or '<p class="empty">No structural gaps found.</p>'

    ambiguity_html = ""
    if ambiguous:
        items = "".join(f"<li><code>{e(m)}</code> → {', '.join(e(t) for t in sorted(ts))}</li>"
                        for m, ts in sorted(ambiguous.items()))
        ambiguity_html = (
            '<div class="callout"><strong>Ambiguous transitions.</strong> These method names are declared on more '
            'than one page object with different return types, so a call in a test is credited to every candidate. '
            f'Rename them if the numbers matter to you.<ul>{items}</ul></div>')

    legend = (
        '<div class="legend">'
        f'<span><i class="swatch" style="background:{STATUS_FILL["opened"]}"></i>opened by URL</span>'
        f'<span><i class="swatch" style="background:{STATUS_FILL["reached"]}"></i>reached by navigation</span>'
        f'<span><i class="swatch" style="background:{STATUS_FILL["untouched"]}"></i>never touched</span>'
        "</div>")

    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>UI route coverage — TeamCity Selenide suite</title>
<style>{CSS}</style></head>
<body><div class="wrap">

<header class="page">
  <div>
    <h1>UI route coverage</h1>
    <p class="sub">Which page objects and URL templates the Selenide suite actually exercises,
    derived statically from <code>{e(args.pages)}</code> and <code>{e(args.tests)}</code>.</p>
  </div>
  <button class="btn" id="theme" type="button">Toggle theme</button>
</header>

<div class="hero">
  <div>
    <div class="figure">{fmt_pct(page_cov, 0)}</div>
    <div class="caption">of concrete page objects are reached by at least one test
    ({len(touched)} of {len(concrete)}). {len(opened)} are opened directly by URL; the rest are only
    ever arrived at by clicking through another page.</div>
  </div>
  <div class="aside">
    <div><div class="v">{fmt_pct(route_cov, 0)}</div><div class="k">route templates hit</div></div>
    <div><div class="v">{test_cases}</div><div class="k">test cases</div></div>
    <div><div class="v">{len(untouched)}</div><div class="k">untouched pages</div></div>
  </div>
</div>

{product_section(inventory, pages) if inventory else ""}

<h2 style="margin:26px 0 10px">Inside the suite</h2>
<p class="sub" style="margin-bottom:14px">Everything below measures the suite against
<strong>its own page objects</strong> ({len(concrete)} of them), not against the product. A page object
can be 100% reached here while most of TeamCity has no page object at all — which is why the section
above exists.</p>

<div class="grid-kpi">{kpis}</div>

<div class="card">
  <h2>Test pressure per page object</h2>
  <p class="sub">Bar length is the number of test files that reach the page. A short grey bar is a page
  the suite never visits.</p>
  <div class="scroll">{svg_page_bars(rows)}</div>
  {legend}
</div>

<div class="card">
  <h2>Page inventory</h2>
  <p class="sub">The denominator, in full. <code>url()</code> values are resolved through the
  <code>extends</code> chain, so subclasses inherit their parent's route.</p>
  <div class="scroll"><table>
    <thead><tr><th>Page object</th><th>Route</th><th>Status</th><th class="num">Tests</th><th>Reached by</th></tr></thead>
    <tbody>{"".join(inv_rows)}</tbody>
  </table></div>
</div>

<div class="card">
  <h2>Coverage gaps</h2>
  {gaps_html}
  {ambiguity_html}
</div>

<div class="card">
  <h2>Per-test-class reach</h2>
  <div class="scroll"><table>
    <thead><tr><th>Test class</th><th class="num">@Test</th><th class="num">Pages</th><th>Pages reached</th></tr></thead>
    <tbody>{"".join(test_rows)}</tbody>
  </table></div>
  {"<p class='note'>Files under the test tree with no <code>@Test</code> at all (drafts, helpers): "
   + ", ".join(f"<code>{e(os.path.basename(f.rel))}</code>" for f in empty_test_files) + ".</p>"
   if empty_test_files else ""}
</div>

<div class="callout">
  <strong>What this number is not.</strong> Reaching a page is not the same as testing it — a page can be
  opened with no assertion made against it. Route coverage is a floor, not a quality measure: it tells you
  where the suite has never been, which is reliable, and says nothing about the depth of what it does when
  it gets there. Pair it with the declared feature inventory for a number worth reporting.
</div>

<footer>
  Generated {e(now)} by <code>scripts/build_route_coverage.py</code>.
  Static analysis of Java sources: comments are stripped before scanning, so commented-out drafts and
  references to deleted classes are excluded. Pages are credited as reached when a test constructs them,
  passes them to <code>getPage(...)</code>, or calls a page-object method whose declared return type is
  that page.{" " + e(args.provenance) if args.provenance else ""}
</footer>

</div><div id="tip"></div><script>{JS}</script></body></html>
"""


# --------------------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pages", default="src/main/java/ui/pages", help="directory holding page objects")
    ap.add_argument("--tests", default="src/test/java", help="root of the test sources to scan")
    ap.add_argument("--inventory", default="ui-page-inventory.json",
                    help="declared inventory of the product's pages; the section is omitted if absent")
    ap.add_argument("--out", default="my_reports/ui-route-coverage.html", help="destination HTML file")
    ap.add_argument("--json", default="my_reports/ui-route-coverage.json", help="destination JSON summary")
    ap.add_argument("--provenance", default="", help="extra provenance sentence for the footer")
    args = ap.parse_args()

    if not os.path.isdir(args.pages):
        sys.exit(f"pages directory not found: {args.pages}")
    if not os.path.isdir(args.tests):
        sys.exit(f"test root not found: {args.tests}")

    pages = collect_pages(args.pages)
    transitions = collect_transitions(pages)
    files, _ = scan_tests(args.tests, pages, transitions)
    ambiguous = {m: ts for m, ts in transitions.items() if len(ts) > 1}
    inventory = load_inventory(args.inventory)

    concrete = [p for p in pages.values() if not p.abstract]
    touched = [p for p in concrete if p.status != "untouched"]
    routes = sorted({p.url for p in concrete if p.url})
    routes_hit = sorted({p.url for p in concrete if p.url and p.status != "untouched"})
    tests = [f for f in files if f.is_ui_test]

    summary = {
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "pages_total": len(concrete),
        "pages_touched": len(touched),
        "pages_opened": len([p for p in concrete if p.status == "opened"]),
        "pages_reached": len([p for p in concrete if p.status == "reached"]),
        "pages_untouched": sorted(p.name for p in concrete
                                  if p.status == "untouched" and not p.subclassed_by),
        "pages_untouched_base_classes": sorted(p.name for p in concrete
                                               if p.status == "untouched" and p.subclassed_by),
        "page_coverage_pct": round(pct(len(touched), len(concrete)), 1),
        "routes_total": len(routes),
        "routes_hit": len(routes_hit),
        "route_coverage_pct": round(pct(len(routes_hit), len(routes)), 1),
        "routes_missing": [r for r in routes if r not in routes_hit],
        "pages_without_route": sorted(p.name for p in concrete if not p.url),
        "test_classes": len(tests),
        "test_cases": sum(f.test_count for f in tests),
        "ambiguous_transitions": {m: sorted(ts) for m, ts in ambiguous.items()},
        "pages": [
            {"name": p.name, "route": p.url, "status": p.status,
             "reached_by": sorted(p.touched_by)}
            for p in sorted(concrete, key=lambda p: p.name)
        ],
    }

    if inventory:
        inv_areas, inv_totals, unknown_refs, unmapped = inventory_stats(inventory, pages)
        summary["product"] = {
            "server": inventory.get("server"),
            "pages_declared": inv_totals["total"],
            "pages_tested": inv_totals["tested"],
            "pages_modeled": inv_totals["modeled"],
            "pages_missing": inv_totals["missing"],
            "pages_conditional": inv_totals["conditional"],
            "product_coverage_pct": round(pct(inv_totals["tested"], inv_totals["total"]), 1),
            "by_area": [
                {"area": a["name"], "source": a["source"], "total": a["total"],
                 "tested": a["tested"], "modeled_only": a["modeled"] - a["tested"], "missing": a["missing"],
                 "not_modeled": [r["name"] for r in a["rows"] if r["grade"] == "missing"]}
                for a in inv_areas
            ],
            "inventory_refs_missing_page_object": [
                {"area": a, "page": p, "expects": r} for a, p, r in unknown_refs],
            "page_objects_not_in_inventory": unmapped,
        }

    for path in (args.out, args.json):
        parent = os.path.dirname(os.path.abspath(path))
        os.makedirs(parent, exist_ok=True)

    with open(args.json, "w", encoding="utf-8") as fh:
        json.dump(summary, fh, indent=2)
        fh.write("\n")

    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(render(pages, files, args, ambiguous, inventory))

    if inventory:
        pr = summary["product"]
        print(f"product        {pr['pages_tested']}/{pr['pages_declared']} pages covered "
              f"({pr['product_coverage_pct']}%)  "
              f"[{pr['pages_modeled'] - pr['pages_tested']} modeled-untested, {pr['pages_missing']} not modeled]")
    print(f"page objects   {summary['pages_touched']}/{summary['pages_total']} reached "
          f"({summary['page_coverage_pct']}%)  "
          f"[{summary['pages_opened']} opened, {summary['pages_reached']} navigation-only]")
    print(f"route templates {summary['routes_hit']}/{summary['routes_total']} hit "
          f"({summary['route_coverage_pct']}%)")
    if summary["pages_untouched"]:
        print("untouched      " + ", ".join(summary["pages_untouched"]))
    print(f"wrote {args.out}")
    print(f"wrote {args.json}")


if __name__ == "__main__":
    main()
