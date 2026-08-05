#!/usr/bin/env python3
"""Build a dual-lens API coverage report from two swagger-coverage runs over the same evidence.

The same recorded requests are measured twice:

    narrow lens   teamcity-swagger-filtered_p1.json   the 20 operations scoped as P1
    wide lens     teamcity-swagger-extended.json      all 449 operations TeamCity exposes

Reading both together answers three questions one spec alone cannot:
  * how well the prioritised surface is covered      (narrow lens)
  * how much of the product that surface represents  (wide lens)
  * where the two lenses disagree                    (scope gaps + spec defects)

Inputs
    two swagger-coverage-results.json files, produced by running
    swagger-coverage-commandline once per spec against the SAME -i input folder.

Output
    a self-contained HTML report. Tokens, chart grammar and class names are lifted
    from build_quality_dashboard.py so the two reports read as one system.

Usage
    python3 scripts/build_dual_spec_coverage.py \
        --narrow out/p1/swagger-coverage-results.json \
        --wide   out/extended/swagger-coverage-results.json \
        --narrow-label "P1 scope" --wide-label "Full API" \
        --out coverage-dual.html
"""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
from datetime import datetime, timezone

MARGIN = 4


# --------------------------------------------------------------------------------------
# Helpers (same contracts as build_quality_dashboard.py)
# --------------------------------------------------------------------------------------
def e(text) -> str:
    return html.escape(str(text), quote=True)


def pct(numerator, denominator):
    if not denominator:
        return None
    return 100.0 * numerator / denominator


def fmt_pct(value, digits=1):
    return "–" if value is None else f"{value:.{digits}f}%"


def read_json(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, ValueError) as exc:
        sys.exit(f"error: cannot read {path}: {exc}")


def rounded_right_path(x, y, w, h, r):
    r = min(r, w, h / 2)
    return (f"M{x:.2f},{y:.2f} H{x + w - r:.2f} Q{x + w:.2f},{y:.2f} {x + w:.2f},{y + r:.2f} "
            f"V{y + h - r:.2f} Q{x + w:.2f},{y + h:.2f} {x + w - r:.2f},{y + h:.2f} "
            f"H{x:.2f} Z")


# --------------------------------------------------------------------------------------
# Extraction
# --------------------------------------------------------------------------------------
def op_key(op):
    """swagger-coverage keys operations as 'PATH METHOD'."""
    k = op.get("operationKey") or {}
    return f"{k.get('path')} {k.get('httpMethod')}"


def extract(data, label):
    """Pull every figure this report needs out of one swagger-coverage-results.json."""
    counter = (data.get("coverageOperationMap") or {}).get("counter") or {}
    conds = data.get("conditionCounter") or {}
    tags = data.get("tagCounter") or {}
    ops = data.get("operations") or {}

    all_ops = int(counter.get("all") or 0)
    full = int(counter.get("full") or 0)
    partial = int(counter.get("party") or 0)     # swagger-coverage spells it "party"
    empty = int(counter.get("empty") or 0)

    touched, defects = {}, []
    for key, op in ops.items():
        calls = int(op.get("processCount") or 0)
        if calls > 0:
            touched[key] = {"calls": calls, "state": op.get("state")}
            for c in op.get("conditions") or []:
                reason = c.get("reason") or ""
                if "Undeclared status" in reason:
                    defects.append({
                        "operation": key,
                        "calls": calls,
                        "reason": reason.strip(),
                        "status": re.sub(r"\D+", "", reason) or "?",
                    })

    predicates = []
    for name, stats in (data.get("conditionStatisticsMap") or {}).items():
        if not isinstance(stats, dict):
            continue
        c_all = int(stats.get("allCount") or 0)
        c_cov = int(stats.get("coveredCount") or 0)
        predicates.append({
            "name": re.sub(r"ConditionPredicate$", "", name),
            "all": c_all,
            "covered": c_cov,
            "rate": pct(c_cov, c_all),
        })
    predicates.sort(key=lambda p: -p["all"])

    domains = []
    for name, tag in (data.get("tagCoverageMap") or {}).items():
        cc = tag.get("coverageCounter") or {}
        tc = tag.get("conditionCounter") or {}
        domains.append({
            "name": name,
            "ops_all": int(cc.get("all") or 0),
            "ops_full": int(cc.get("full") or 0),
            "ops_partial": int(cc.get("party") or 0),
            "ops_empty": int(cc.get("empty") or 0),
            "conds_all": int(tc.get("all") or 0),
            "conds_covered": int(tc.get("covered") or 0),
            "calls": int(tag.get("callCounts") or 0),
            "state": tag.get("state"),
        })
    domains.sort(key=lambda d: (-d["calls"], -d["ops_all"]))

    return {
        "label": label,
        "operations_all": all_ops,
        "full": full,
        "partial": partial,
        "empty": empty,
        "reached": len(touched),
        "reach_rate": pct(len(touched), all_ops),
        "op_coverage": pct(full + partial, all_ops),
        "conditions_all": int(conds.get("all") or 0),
        "conditions_covered": int(conds.get("covered") or 0),
        "conditions_rate": pct(int(conds.get("covered") or 0), int(conds.get("all") or 0)),
        "tags_all": int(tags.get("all") or 0),
        "tags_full": int(tags.get("full") or 0),
        "tags_partial": int(tags.get("party") or 0),
        "tags_empty": int(tags.get("empty") or 0),
        "touched": touched,
        "defects": defects,
        "predicates": predicates,
        "domains": domains,
        "generated_at": (data.get("generationStatistics") or {}).get("generateDate"),
    }


def declared_ops(data):
    """Every operation the spec declares, as 'PATH METHOD' keys."""
    return set((data.get("operations") or {}).keys())


# --------------------------------------------------------------------------------------
# Charts
# --------------------------------------------------------------------------------------
def svg_coverage_bars(lenses, height=124):
    """One stacked bar per lens: full / partial / empty operations.

    Part-to-whole on an *ordered* scale -> ordinal one-hue ramp (validated ALL CHECKS
    PASS light and dark), never categorical hues. Both bars share the full width so the
    reader compares proportion, and the absolute totals are stated in the row label.
    """
    width, pad_l, pad_r = 720, 0, 0
    bar_w = width - pad_l - pad_r
    bar_h, gap, row_gap = 30, 2.0, 56

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" '
           f'aria-label="Operation coverage by spec: full, partial and empty">']

    for row, lens in enumerate(lenses):
        total = lens["operations_all"] or 1
        y = 22 + row * row_gap
        out.append(f'<text x="0" y="{y - 7:.2f}" class="rowlabel">'
                   f'{e(lens["label"])} — {lens["operations_all"]} operations</text>')
        segs = [("full", "Full", lens["full"], "var(--cov-full)"),
                ("partial", "Partial", lens["partial"], "var(--cov-partial)"),
                ("empty", "Empty", lens["empty"], "var(--cov-empty)")]
        drawn = [s for s in segs if s[2] > 0]
        cursor = float(pad_l)
        for order, (key, label, count, color) in enumerate(drawn):
            raw_w = bar_w * count / total
            is_end = order == len(drawn) - 1
            seg_w = raw_w if is_end else max(raw_w - gap, 0.6)
            share = pct(count, total)
            tip = f"{lens['label']} — {label}: {count} of {total} operations ({share:.1f}%)"
            if is_end:
                out.append(f'<path d="{rounded_right_path(cursor, y, seg_w, bar_h, 4)}" '
                           f'fill="{color}" data-tip="{e(tip)}" class="mark"/>')
            else:
                out.append(f'<rect x="{cursor:.2f}" y="{y:.2f}" width="{seg_w:.2f}" '
                           f'height="{bar_h}" fill="{color}" data-tip="{e(tip)}" class="mark"/>')
            if seg_w >= 30:
                ink = "var(--on-fill-light)" if key == "empty" else "var(--on-fill-dark)"
                out.append(f'<text x="{cursor + seg_w / 2:.2f}" y="{y + bar_h / 2 + 4.5:.2f}" '
                           f'class="segval" fill="{ink}" text-anchor="middle">{count}</text>')
            cursor += raw_w
    out.append("</svg>")
    return "".join(out)


def svg_reach_bars(rows, height_per=24):
    """Horizontal bars — recorded calls per endpoint. Magnitude -> sequential single hue.

    In-scope endpoints take the accent; endpoints outside the narrow spec take the
    de-emphasis tone, so the scope gap is visible in the mark itself and not only in
    the table. Identity never rests on color alone: the out-of-scope rows are also
    labelled in the trailing column and repeated in the table below.
    """
    if not rows:
        return '<p class="empty">No endpoint was reached by any recorded request.</p>'

    width, label_w, pad_r = 720, 336, 96
    bar_h, row_h = 13, height_per
    height = row_h * len(rows) + 10
    plot_w = width - label_w - pad_r
    top = max(r["calls"] for r in rows) or 1

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" '
           f'aria-label="Recorded calls per endpoint reached by the suite">']
    for idx, r in enumerate(rows):
        y = 5 + idx * row_h
        w = plot_w * r["calls"] / top
        color = "var(--accent)" if r["in_scope"] else "var(--deemph)"
        scope = "in P1 scope" if r["in_scope"] else "outside P1 scope"
        out.append(f'<text x="0" y="{y + bar_h / 2 + 4:.2f}" class="rowlabel">'
                   f'{e(shorten(r["operation"], 46))}</text>')
        out.append(f'<path d="{rounded_right_path(label_w, y, max(w, 1), bar_h, 4)}" '
                   f'fill="{color}" class="mark" '
                   f'data-tip="{e(r["operation"])} — {r["calls"]} calls, {scope}"/>')
        out.append(f'<text x="{label_w + w + 8:.2f}" y="{y + bar_h / 2 + 4:.2f}" '
                   f'class="barval">{r["calls"]}</text>')
    out.append("</svg>")
    return "".join(out)


def shorten(name, limit):
    name = name.replace("/app/rest", "")
    return name if len(name) <= limit else name[: limit - 1] + "…"


# --------------------------------------------------------------------------------------
# Page
# --------------------------------------------------------------------------------------
CSS = """
*,*::before,*::after{box-sizing:border-box}
:root{
  color-scheme:light;
  --surface-0:#f4f3f0; --surface-1:#fcfcfb; --border:#e2e0da;
  --text-primary:#0b0b0b; --text-secondary:#52514e; --text-muted:#83817a;
  --accent:#2a78d6; --deemph:#c9c7c0;
  --cov-full:#1c5cab; --cov-partial:#3987e5; --cov-empty:#86b6ef;
  --on-fill-dark:#ffffff; --on-fill-light:#0b0b0b;
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
    --on-fill-dark:#ffffff; --on-fill-light:#ffffff;
    --good-ink:#48c048; --warn-ink:#fab219; --bad-ink:#e66767;
  }
}
:root[data-theme="dark"]{
  color-scheme:dark;
  --surface-0:#121211; --surface-1:#1a1a19; --border:#333330;
  --text-primary:#ffffff; --text-secondary:#c3c2b7; --text-muted:#96958c;
  --accent:#3987e5; --deemph:#4a4a46;
  --cov-full:#184f95; --cov-partial:#2a78d6; --cov-empty:#6da7ec;
  --on-fill-dark:#ffffff; --on-fill-light:#ffffff;
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
h3{font-size:14px;margin:0 0 10px;font-weight:600;color:var(--text-secondary)}
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
.card>.chart,.card>.scroll>.chart{margin-top:12px}
.chart{width:100%;height:auto;display:block;overflow:visible}
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
td.wrapname{white-space:normal;min-width:220px;overflow-wrap:anywhere}
code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12.5px}
.pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;padding:2px 8px;border-radius:99px;border:1px solid var(--border)}
.pill.good{color:var(--good-ink)} .pill.warn{color:var(--warn-ink)} .pill.bad{color:var(--bad-ink)} .pill.none{color:var(--text-muted)}
.callout{border:1px solid var(--border);border-left:3px solid var(--warn);border-radius:8px;
  background:var(--surface-0);padding:13px 16px;margin-top:14px;font-size:13.5px;color:var(--text-secondary);max-width:82ch}
.callout strong{color:var(--text-primary)}
.rowlabel{fill:var(--text-secondary);font-size:12.5px}
.barval{fill:var(--text-primary);font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}
.segval{font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}
.mark{cursor:pointer}
.mark:hover{opacity:.82}
.empty{color:var(--text-muted);font-size:13px;margin:10px 0 0}
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


def tile(label, value, note=""):
    note_html = f'<div class="note">{e(note)}</div>' if note else ""
    return (f'<div class="tile"><div class="label">{e(label)}</div>'
            f'<div class="value">{e(value)}</div>{note_html}</div>')


TITLE_RE = re.compile(r"<title>(.*?)</title>\s*", re.S)


def standalone(body):
    """Wrap the artifact fragment into a complete document for opening from disk.

    The default output omits doctype/head/body because the Artifact publisher supplies
    them. A file opened with file:// gets neither, so it renders in quirks mode and,
    with no charset declared, mangles the typographic characters in the copy.
    """
    m = TITLE_RE.search(body)
    title = m.group(1) if m else "API coverage"
    body = TITLE_RE.sub("", body, count=1)
    return (f"<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
            f"<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
            f"<title>{title}</title>\n</head>\n<body>\n{body}\n</body>\n</html>\n")


def render(narrow, wide, reach_rows, defects, divergent, out_of_scope, args):
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    scope_share = pct(narrow["operations_all"], wide["operations_all"])

    # ---- reach table -------------------------------------------------------------
    scope_cell = {True: "In P1 spec",
                  False: '<span class="pill warn">△ Outside P1</span>'}
    reach_tbl = "".join(
        f'<tr><td class="wrapname"><code>{e(r["operation"])}</code></td>'
        f'<td class="num">{r["calls"]}</td>'
        f'<td>{scope_cell[r["in_scope"]]}</td>'
        f'<td>{e(r["narrow_state"] or "not declared")}</td>'
        f'<td>{e(r["wide_state"])}</td></tr>'
        for r in reach_rows)

    # ---- domain table ------------------------------------------------------------
    wide_by_name = {d["name"]: d for d in wide["domains"]}
    dom_rows = []
    for d in narrow["domains"]:
        w = wide_by_name.get(d["name"])
        dom_rows.append(
            f'<tr><td>{e(d["name"])}</td>'
            f'<td class="num">{d["ops_partial"] + d["ops_full"]}/{d["ops_all"]}</td>'
            f'<td class="num">{d["conds_covered"]}/{d["conds_all"]}</td>'
            f'<td class="num">{(w["ops_partial"] + w["ops_full"]) if w else 0}/{w["ops_all"] if w else "–"}</td>'
            f'<td class="num">{w["conds_covered"] if w else 0}/{w["conds_all"] if w else "–"}</td>'
            f'<td class="num">{d["calls"]}</td></tr>')
    # Domains that exist only in the wide lens but were actually called.
    for w in wide["domains"]:
        if w["name"] not in {d["name"] for d in narrow["domains"]} and w["calls"] > 0:
            dom_rows.append(
                f'<tr><td>{e(w["name"])} <span class="pill warn">△ Outside P1</span></td>'
                f'<td class="num muted">not declared</td><td class="num muted">–</td>'
                f'<td class="num">{w["ops_partial"] + w["ops_full"]}/{w["ops_all"]}</td>'
                f'<td class="num">{w["conds_covered"]}/{w["conds_all"]}</td>'
                f'<td class="num">{w["calls"]}</td></tr>')

    # ---- predicate table ---------------------------------------------------------
    wide_preds = {p["name"]: p for p in wide["predicates"]}
    pred_rows = []
    for p in narrow["predicates"]:
        w = wide_preds.get(p["name"])
        pred_rows.append(
            f'<tr><td>{e(p["name"])}</td>'
            f'<td class="num">{p["covered"]}/{p["all"]}</td><td class="num">{fmt_pct(p["rate"])}</td>'
            f'<td class="num">{w["covered"] if w else 0}/{w["all"] if w else "–"}</td>'
            f'<td class="num">{fmt_pct(w["rate"]) if w else "–"}</td></tr>')

    # ---- defect table ------------------------------------------------------------
    defect_tbl = "".join(
        f'<tr><td class="wrapname"><code>{e(d["operation"])}</code></td>'
        f'<td class="num">{d["calls"]}</td>'
        f'<td class="num">{e(d["status"])}</td>'
        f'<td>{e(d["narrow_state"])}</td><td>{e(d["wide_state"])}</td></tr>'
        for d in defects)

    return f"""<title>API coverage — two lenses on the same evidence</title>
<style>{CSS}</style>
<div class="wrap">
<header class="page">
  <div>
    <h1>API coverage: two lenses on the same evidence</h1>
    <p class="sub">The same {args.requests} recorded requests, measured twice — once against the
    {narrow["operations_all"]} operations scoped as P1, once against all {wide["operations_all"]}
    operations TeamCity exposes. One number answers “are we covering what we chose to cover”; the
    other answers “how much of the product is that”.</p>
  </div>
  <button class="btn" id="theme" type="button">Toggle theme</button>
</header>

<div class="hero">
  <div>
    <div class="figure">{fmt_pct(narrow["op_coverage"], 0)}</div>
    <div class="caption"><strong>of the P1 surface is exercised</strong> — {narrow["full"] + narrow["partial"]}
    of {narrow["operations_all"]} operations. Measured against the whole API instead, the same suite
    covers {fmt_pct(wide["op_coverage"])}. Neither figure is wrong; they answer different questions.</div>
  </div>
  <div class="aside">
    <div><div class="v">{fmt_pct(scope_share)}</div><div class="k">P1 share of the API</div></div>
    <div><div class="v">{narrow["conditions_covered"]}/{narrow["conditions_all"]}</div><div class="k">P1 conditions</div></div>
    <div><div class="v">{wide["conditions_covered"]}/{wide["conditions_all"]}</div><div class="k">All-API conditions</div></div>
    <div><div class="v">{len(reach_rows)}</div><div class="k">endpoints reached</div></div>
  </div>
</div>

<div class="grid-kpi">
  {tile("P1 operations covered", f'{narrow["full"] + narrow["partial"]}/{narrow["operations_all"]}', f'{fmt_pct(narrow["op_coverage"])} — full or partial')}
  {tile("Full-coverage operations", f'{narrow["full"]}/{narrow["operations_all"]}', "every declared condition met")}
  {tile("P1 conditions met", fmt_pct(narrow["conditions_rate"]), f'{narrow["conditions_covered"]} of {narrow["conditions_all"]} — the strict signal')}
  {tile("All-API operations covered", f'{wide["full"] + wide["partial"]}/{wide["operations_all"]}', fmt_pct(wide["op_coverage"]))}
  {tile("All-API conditions met", fmt_pct(wide["conditions_rate"], 2), f'{wide["conditions_covered"]} of {wide["conditions_all"]}')}
  {tile("Domains touched", f'{wide["tags_full"] + wide["tags_partial"]}/{wide["tags_all"]}', "TeamCity API tags with any call")}
</div>

<div class="card">
  <h2>Operation coverage under each lens</h2>
  <p class="sub">Identical evidence, two denominators. The P1 bar is the one to hold the team to;
  the full-API bar is context for how narrow that commitment is.</p>
  <div class="scroll">{svg_coverage_bars([narrow, wide])}</div>
  <div class="legend">
    <span><i class="swatch" style="background:var(--cov-full)"></i>Full — every declared condition met</span>
    <span><i class="swatch" style="background:var(--cov-partial)"></i>Partial — called, some conditions met</span>
    <span><i class="swatch" style="background:var(--cov-empty)"></i>Empty — never called, or no condition met</span>
  </div>
  <p class="note">No operation reaches <strong>Full</strong> under either lens. Every endpoint the suite
  touches is exercised on its happy path only — the declared 401/403/404 branches and parameter
  variants are untested. That, not the headline percentage, is the honest weak spot.</p>
</div>

<div class="card">
  <h2>What the suite actually reaches</h2>
  <p class="sub">Every operation with at least one recorded request, by call volume. Bars in the accent
  are declared in the P1 spec; grey bars are endpoints the suite exercises that P1 never claimed.</p>
  <div class="scroll">{svg_reach_bars(reach_rows)}</div>
  <div class="legend">
    <span><i class="swatch" style="background:var(--accent)"></i>Declared in P1 spec</span>
    <span><i class="swatch" style="background:var(--deemph)"></i>Outside P1 scope</span>
  </div>
  <div class="callout">
    <strong>Scope gap:</strong> {len(out_of_scope)} of the {len(reach_rows)} endpoints the suite reaches
    are not in the P1 spec — mostly fixture and cleanup traffic (user deletion, agent authorisation,
    project listing). They are tested in practice but invisible to the P1 number. Either widen the P1
    spec to declare them, or accept that the headline understates real reach.
  </div>
  <div class="scroll" style="margin-top:14px">
    <table>
      <thead><tr><th>Operation</th><th class="num">Calls</th><th>Scope</th><th>P1 verdict</th><th>Full-API verdict</th></tr></thead>
      <tbody>{reach_tbl}</tbody>
    </table>
  </div>
</div>

<div class="card">
  <h2>Where the two lenses disagree — and why</h2>
  <p class="sub">{len(defects)} operations are counted as covered under P1 but read as
  <em>empty</em> against the stock TeamCity spec. The tests did not change; the spec did.</p>
  <div class="scroll">
    <table>
      <thead><tr><th>Operation</th><th class="num">Calls</th><th class="num">Status returned</th><th>P1 verdict</th><th>Full-API verdict</th></tr></thead>
      <tbody>{defect_tbl}</tbody>
    </table>
  </div>
  <div class="callout">
    <strong>This is a spec defect, not a test gap.</strong> TeamCity really returns
    <code>204</code> on these deletes (and <code>503</code> while agents are still syncing), but the
    stock <code>teamcity-swagger-extended.json</code> declares only 401/403/404/default. swagger-coverage
    therefore records “Undeclared status” and refuses the operation any credit. The filtered P1 spec was
    corrected to declare those statuses, which is exactly why it scores them. Read the full-API column as
    a measure of <em>spec accuracy</em> as much as of test coverage.
  </div>
</div>

<div class="card">
  <h2>Coverage by domain</h2>
  <p class="sub">TeamCity API tags under both lenses. The call column shows where the suite spends its
  requests — a domain with heavy traffic but low condition coverage is tested shallowly, not thoroughly.</p>
  <div class="scroll">
    <table>
      <thead><tr>
        <th>Domain</th>
        <th class="num">P1 ops</th><th class="num">P1 conditions</th>
        <th class="num">All-API ops</th><th class="num">All-API conditions</th>
        <th class="num">Calls</th>
      </tr></thead>
      <tbody>{"".join(dom_rows)}</tbody>
    </table>
  </div>
</div>

<div class="card">
  <h2>What kind of condition goes untested</h2>
  <p class="sub">swagger-coverage splits every operation into condition types. This is the breakdown that
  explains the zero in the “Full” column.</p>
  <div class="scroll">
    <table>
      <thead><tr><th>Condition type</th>
        <th class="num">P1 met</th><th class="num">P1 rate</th>
        <th class="num">All-API met</th><th class="num">All-API rate</th></tr></thead>
      <tbody>{"".join(pred_rows)}</tbody>
    </table>
  </div>
  <p class="note"><strong>Status</strong> conditions dominate and are the least covered: the suite asserts
  success codes and almost no error codes. <strong>Parameter</strong> conditions — optional query and path
  variants — are barely touched. Both are cheap to improve and would move the strict number fastest.</p>
</div>

<div class="card">
  <h2>How to read these numbers</h2>
  <p class="sub">Three rules keep this report honest in a review.</p>
  <ol class="sub" style="padding-left:20px;line-height:1.85">
    <li><strong>Quote the P1 figure with its denominator.</strong> “{fmt_pct(narrow["op_coverage"], 0)} of
    the {narrow["operations_all"]} operations we scoped as P1” — never a bare percentage, which invites the
    reader to assume it covers the whole API.</li>
    <li><strong>Treat conditions, not operations, as the real signal.</strong> An operation counts as
    “partial” after a single happy-path call. {fmt_pct(narrow["conditions_rate"])} of P1 conditions met is
    the number that reflects testing depth.</li>
    <li><strong>Don’t chase the full-API percentage.</strong> {fmt_pct(wide["op_coverage"])} is a scope
    statement, not a failure. Raising it means widening P1 deliberately — and fixing the spec defects above
    first, since {len(defects)} operations are already tested and simply not credited.</li>
  </ol>
</div>

<footer>
  Generated {e(now)} by <code>scripts/build_dual_spec_coverage.py</code>.
  Evidence: {args.requests} request recordings in <code>{e(args.input_label)}</code>, measured with
  swagger-coverage-commandline against <code>teamcity-swagger-filtered_p1.json</code> and
  <code>teamcity-swagger-extended.json</code>.
  {e(args.provenance)}
</footer>
</div>
<div id="tip"></div>
<script>{JS}</script>
"""


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--narrow", required=True, help="swagger-coverage-results.json for the filtered P1 spec")
    ap.add_argument("--wide", required=True, help="swagger-coverage-results.json for the extended spec")
    ap.add_argument("--narrow-label", default="P1 scope")
    ap.add_argument("--wide-label", default="Full TeamCity API")
    ap.add_argument("--out", required=True, help="destination HTML file")
    ap.add_argument("--standalone", action="store_true",
                    help="emit a complete HTML document (doctype/head/body) for opening from disk; "
                         "omit when the output is going to be published as an artifact, which "
                         "supplies its own wrapper")
    ap.add_argument("--requests", default="?", help="number of recorded request files, for the provenance line")
    ap.add_argument("--input-label", default="target/swagger-coverage-output")
    ap.add_argument("--provenance", default="", help="extra provenance sentence (run number, branch, caveats)")
    args = ap.parse_args()

    narrow = extract(read_json(args.narrow), args.narrow_label)
    wide = extract(read_json(args.wide), args.wide_label)

    narrow_declared = declared_ops(read_json(args.narrow))

    # Endpoint reach is a property of the widest lens: the narrow spec cannot see
    # operations it never declared.
    reach_rows = []
    for key, info in sorted(wide["touched"].items(), key=lambda kv: -kv[1]["calls"]):
        in_scope = key in narrow_declared
        reach_rows.append({
            "operation": key,
            "calls": info["calls"],
            "in_scope": in_scope,
            "narrow_state": (narrow["touched"].get(key) or {}).get("state") if in_scope else None,
            "wide_state": info["state"],
        })
    out_of_scope = [r for r in reach_rows if not r["in_scope"]]

    # Same evidence, different verdict -> the spec, not the suite, is the variable.
    defects = []
    for d in wide["defects"]:
        key = d["operation"]
        n = narrow["touched"].get(key)
        defects.append({**d,
                        "narrow_state": (n or {}).get("state") or "not declared in P1",
                        "wide_state": wide["touched"][key]["state"]})
    defects.sort(key=lambda d: -d["calls"])

    divergent = [r for r in reach_rows
                 if r["narrow_state"] and r["narrow_state"] != r["wide_state"]]

    body = render(narrow, wide, reach_rows, defects, divergent, out_of_scope, args)
    if args.standalone:
        body = standalone(body)

    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(body)

    print(f"wrote {args.out}")
    print(f"  {args.narrow_label}: {narrow['full'] + narrow['partial']}/{narrow['operations_all']} ops "
          f"({fmt_pct(narrow['op_coverage'])}), conditions {narrow['conditions_covered']}/{narrow['conditions_all']}")
    print(f"  {args.wide_label}: {wide['full'] + wide['partial']}/{wide['operations_all']} ops "
          f"({fmt_pct(wide['op_coverage'])}), conditions {wide['conditions_covered']}/{wide['conditions_all']}")
    print(f"  endpoints reached {len(reach_rows)}, outside P1 {len(out_of_scope)}, spec defects {len(defects)}")


if __name__ == "__main__":
    main()
