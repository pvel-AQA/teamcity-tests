#!/usr/bin/env python3
"""Build a self-contained Quality Report from published Allure reports and swagger-coverage results.

Read-only aggregation over artifacts CI already produces. No test changes, no new
services, stdlib only.

Inputs
    <site-dir>/<run>/widgets/summary.json          per-run pass/fail/broken/skipped
    <site-dir>/<run>/widgets/executors.json        build order + Actions run URL
    <site-dir>/<run>/data/test-cases/*.json        per-test status, duration, historyId, suite
    <site-dir>/<run>/swagger-coverage-results.json  API coverage (optional, per run)
    <site-dir>/quality/history.json                rolling history that outlives report pruning

Outputs
    <site-dir>/<destination>/index.html            the dashboard
    <site-dir>/<destination>/summary.json          machine-readable headline metrics
    <site-dir>/<destination>/history.json          updated rolling history
    --markdown-out                                 Markdown digest for $GITHUB_STEP_SUMMARY

Usage
    python3 scripts/build_quality_dashboard.py \
        --site-dir allure-history --gates quality-gates.json \
        --repo owner/name --run-number 360 \
        --swagger-results swagger-coverage-results.json
"""

from __future__ import annotations

import argparse
import html
import json
import math
import os
import re
import sys
from datetime import datetime, timezone

# --------------------------------------------------------------------------------------
# Palette. Validated with the dataviz skill's validate_palette.js in both modes.
#   status set, in stack order passed -> skipped -> failed -> broken:
#     CVD separation PASS (worst adjacent dE 11.3), normal-vision PASS (worst 15.7).
#   The status palette is fixed and never themed; its sub-3:1 light-surface contrast is
#   mitigated by the relief rule - every status ships an icon + label + the table view.
#   ordinal blue ramp (full/partial/empty): ALL CHECKS PASS light and dark.
# --------------------------------------------------------------------------------------
STATUS_COLORS = {
    "passed": "#0ca30c",   # status: good
    "skipped": "#fab219",  # status: warning
    "failed": "#d03b3b",   # status: critical
    "broken": "#ec835a",   # status: serious
    "unknown": "#8a8a85",
}
# Stack / legend order is the validated adjacency order - do not reorder casually.
STATUS_ORDER = ["passed", "skipped", "failed", "broken"]
STATUS_ICONS = {"passed": "●", "skipped": "◐", "failed": "✕", "broken": "⚠"}

GATE_ICONS = {"good": "✓", "warn": "△", "bad": "✕", "none": "–"}

MS = 1000.0


# --------------------------------------------------------------------------------------
# Small helpers
# --------------------------------------------------------------------------------------
def e(text) -> str:
    """Escape for HTML text and attribute contexts."""
    return html.escape(str(text), quote=True)


def read_json(path, default=None):
    """Read JSON, returning `default` on any absence or corruption.

    Published reports are not uniformly shaped - runs 341/352/353 carry a summary.json
    with no time.duration - so every read here is best-effort by design.
    """
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, ValueError):
        return default


def pct(numerator, denominator):
    """Percentage, or None when the denominator is zero (never a fake 0%)."""
    if not denominator:
        return None
    return 100.0 * numerator / denominator


def fmt_pct(value, digits=1):
    return "–" if value is None else f"{value:.{digits}f}%"


def fmt_secs(value, digits=1):
    if value is None:
        return "–"
    if value >= 90:
        return f"{value / 60:.1f} min"
    return f"{value:.{digits}f}s"


def fmt_int(value):
    return "–" if value is None else f"{value:,}"


def mean(values):
    values = [v for v in values if v is not None]
    return sum(values) / len(values) if values else None


def percentile(values, p):
    values = sorted(v for v in values if v is not None)
    if not values:
        return None
    k = (len(values) - 1) * (p / 100.0)
    lo, hi = math.floor(k), math.ceil(k)
    if lo == hi:
        return values[int(k)]
    return values[lo] * (hi - k) + values[hi] * (k - lo)


def short_name(name, limit=44):
    name = re.sub(r"\(\)$", "", str(name or ""))
    return name if len(name) <= limit else name[: limit - 1] + "…"


# --------------------------------------------------------------------------------------
# Loading
# --------------------------------------------------------------------------------------
class Run:
    """One published Allure report directory."""

    def __init__(self, number, path):
        self.number = number
        self.path = path
        self.stat = {}
        self.total = 0
        self.passed = self.failed = self.broken = self.skipped = self.unknown = 0
        self.executed = 0
        self.wall_seconds = None      # suite start -> stop
        self.sum_seconds = None       # summed per-test durations
        self.started_at = None
        self.build_url = None
        self.report_url = None
        self.tests = []               # per-test records
        self.suites = []              # per-suite statistics
        self.coverage = None          # swagger-coverage metrics, when published

    # -- derived rates ---------------------------------------------------------------
    @property
    def pass_rate(self):
        return pct(self.passed, self.executed)

    @property
    def fail_rate(self):
        return pct(self.failed, self.executed)

    @property
    def broken_rate(self):
        return pct(self.broken, self.executed)

    @property
    def skip_rate(self):
        return pct(self.skipped, self.total)

    @property
    def test_coverage(self):
        return pct(self.executed, self.total)

    @property
    def avg_duration(self):
        return self.sum_seconds / self.executed if self.sum_seconds and self.executed else None

    @property
    def critical_failures(self):
        """Failed + broken, narrowing to blocker/critical once @Severity annotations exist.

        Allure defaults every test to severity 'normal'. Treating that default as
        'critical' would be wrong, so the narrow definition only kicks in when the run
        actually carries non-default severities.
        """
        graded = [t for t in self.tests if t.get("severity") not in (None, "normal")]
        if graded:
            return sum(
                1 for t in self.tests
                if t.get("status") in ("failed", "broken")
                and t.get("severity") in ("blocker", "critical")
            )
        return self.failed + self.broken


def load_run(site_dir, number):
    path = os.path.join(site_dir, str(number))
    summary = read_json(os.path.join(path, "widgets", "summary.json"))
    if not isinstance(summary, dict) or "statistic" not in summary:
        return None

    run = Run(number, path)
    stat = summary.get("statistic") or {}
    run.stat = stat
    run.total = int(stat.get("total") or 0)
    run.passed = int(stat.get("passed") or 0)
    run.failed = int(stat.get("failed") or 0)
    run.broken = int(stat.get("broken") or 0)
    run.skipped = int(stat.get("skipped") or 0)
    run.unknown = int(stat.get("unknown") or 0)
    run.executed = max(run.total - run.skipped, 0)

    time = summary.get("time") or {}
    # Defensive: some published summaries omit duration/sumDuration entirely.
    if isinstance(time.get("duration"), (int, float)):
        run.wall_seconds = time["duration"] / MS
    elif isinstance(time.get("start"), (int, float)) and isinstance(time.get("stop"), (int, float)):
        run.wall_seconds = max(time["stop"] - time["start"], 0) / MS
    if isinstance(time.get("sumDuration"), (int, float)):
        run.sum_seconds = time["sumDuration"] / MS
    if isinstance(time.get("start"), (int, float)):
        run.started_at = datetime.fromtimestamp(time["start"] / MS, tz=timezone.utc)

    executors = read_json(os.path.join(path, "widgets", "executors.json"), [])
    if isinstance(executors, list) and executors:
        run.build_url = executors[0].get("buildUrl")
        run.report_url = executors[0].get("reportUrl")

    run.tests = load_test_cases(path)
    if run.sum_seconds is None and run.tests:
        run.sum_seconds = sum(t["duration"] for t in run.tests) / MS

    suites = read_json(os.path.join(path, "widgets", "suites.json"), {})
    if isinstance(suites, dict):
        run.suites = [i for i in (suites.get("items") or []) if isinstance(i, dict)]

    run.coverage = load_coverage(os.path.join(path, "swagger-coverage-results.json"))
    return run


def load_test_cases(run_path):
    """Per-test records, preferring data/test-cases (has historyId + suite labels).

    Falls back to widgets/duration.json, which carries name/status/duration/severity but
    no stable cross-run identity.
    """
    tests = []
    tc_dir = os.path.join(run_path, "data", "test-cases")
    if os.path.isdir(tc_dir):
        for entry in sorted(os.listdir(tc_dir)):
            if not entry.endswith(".json"):
                continue
            data = read_json(os.path.join(tc_dir, entry))
            if not isinstance(data, dict):
                continue
            labels = {}
            for label in data.get("labels") or []:
                if isinstance(label, dict) and label.get("name"):
                    labels.setdefault(label["name"], label.get("value"))
            full_name = data.get("fullName") or data.get("name") or entry
            tests.append({
                "name": data.get("name") or full_name,
                "full_name": full_name,
                "identity": data.get("historyId") or full_name,
                "status": data.get("status"),
                "duration": float((data.get("time") or {}).get("duration") or 0),
                "severity": labels.get("severity"),
                "suite": labels.get("suite") or labels.get("parentSuite") or "(no suite)",
                "flaky": bool(data.get("flaky")),
                "retries": int(data.get("retriesCount") or 0),
            })
    if tests:
        return tests

    for item in read_json(os.path.join(run_path, "widgets", "duration.json"), []) or []:
        if not isinstance(item, dict):
            continue
        name = item.get("name") or item.get("uid")
        tests.append({
            "name": name,
            "full_name": name,
            "identity": name,
            "status": item.get("status"),
            "duration": float((item.get("time") or {}).get("duration") or 0),
            "severity": item.get("severity"),
            "suite": "(no suite)",
            "flaky": False,
            "retries": 0,
        })
    return tests


def load_coverage(path):
    """Extract the coverage metrics from a swagger-coverage-results.json."""
    data = read_json(path)
    if not isinstance(data, dict):
        return None

    counter = (data.get("coverageOperationMap") or {}).get("counter") or {}
    conditions = data.get("conditionCounter") or {}
    tags = data.get("tagCounter") or {}

    all_ops = int(counter.get("all") or 0)
    full = int(counter.get("full") or 0)
    partial = int(counter.get("party") or 0)   # swagger-coverage spells it "party"
    empty = int(counter.get("empty") or 0)
    deprecated = int(counter.get("deprecated") or 0)

    cond_all = int(conditions.get("all") or 0)
    cond_covered = int(conditions.get("covered") or 0)

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

    return {
        "operations_all": all_ops,
        "full": full,
        "partial": partial,
        "empty": empty,
        "deprecated": deprecated,
        "api_coverage": pct(full + partial, all_ops),
        "full_coverage_rate": pct(full, all_ops),
        "partial_coverage_rate": pct(partial, all_ops),
        "empty_coverage_rate": pct(empty, all_ops),
        "conditions_all": cond_all,
        "conditions_covered": cond_covered,
        "conditions_coverage": pct(cond_covered, cond_all),
        "tags_all": int(tags.get("all") or 0),
        "tags_full": int(tags.get("full") or 0),
        "tags_partial": int(tags.get("party") or 0),
        "tags_empty": int(tags.get("empty") or 0),
        "predicates": predicates,
        "uncovered": sorted(str(op) for op in (data.get("zeroCall") or []))[:40],
        "generated_at": (data.get("generationStatistics") or {}).get("generateDate"),
    }


def load_ui_coverage(path):
    """Extract UI page/route coverage from scripts/build_route_coverage.py's JSON.

    That script derives the denominator statically from the page objects under
    src/main/java/ui/pages, since a UI has no swagger.json to measure against. It
    grades each page object:
        opened     a test navigates to it by URL      (new X().open(...))
        reached    a test lands on it some other way  (getPage(X.class), transition)
        untouched  no test reaches it at all
    """
    data = read_json(path)
    if not isinstance(data, dict) or not data.get("pages_total"):
        return None

    total = int(data.get("pages_total") or 0)
    opened = int(data.get("pages_opened") or 0)
    reached = int(data.get("pages_reached") or 0)
    # Derive rather than trust len(pages_untouched): that list excludes abstract base
    # classes, which are reported separately but still count against the denominator.
    untouched = max(total - opened - reached, 0)

    routes_total = int(data.get("routes_total") or 0)
    routes_hit = int(data.get("routes_hit") or 0)

    return {
        "pages_total": total,
        "pages_opened": opened,
        "pages_reached": reached,
        "pages_touched": int(data.get("pages_touched") or (opened + reached)),
        "pages_untouched_count": untouched,
        "pages_untouched": list(data.get("pages_untouched") or []),
        "pages_untouched_base_classes": list(data.get("pages_untouched_base_classes") or []),
        "page_coverage": pct(opened + reached, total),
        "routes_total": routes_total,
        "routes_hit": routes_hit,
        "route_coverage": pct(routes_hit, routes_total),
        "routes_missing": list(data.get("routes_missing") or []),
        "pages_without_route": list(data.get("pages_without_route") or []),
        "test_classes": int(data.get("test_classes") or 0),
        "test_cases": int(data.get("test_cases") or 0),
        "pages": [p for p in (data.get("pages") or []) if isinstance(p, dict)],
        "generated_at": data.get("generated"),
    }


def discover_runs(site_dir, limit):
    numbers = []
    if os.path.isdir(site_dir):
        for entry in os.listdir(site_dir):
            if entry.isdigit() and os.path.isdir(os.path.join(site_dir, entry)):
                numbers.append(int(entry))
    numbers.sort()
    runs = [r for r in (load_run(site_dir, n) for n in numbers) if r is not None]
    return runs[-limit:] if limit else runs


# --------------------------------------------------------------------------------------
# Aggregation
# --------------------------------------------------------------------------------------
def aggregate_tests(runs):
    """Per-test history across the window, keyed by stable identity.

    In-run retries are not configured (retriesCount is 0 everywhere), so flakiness is
    detected *across* runs: a test whose status flips between passed and failed/broken
    without an intervening code change is flaky.
    """
    by_identity = {}
    for run in runs:
        for test in run.tests:
            rec = by_identity.setdefault(test["identity"], {
                "name": test["name"],
                "full_name": test["full_name"],
                "suite": test["suite"],
                "statuses": [],
                "durations": [],
                "runs": [],
                "allure_flaky": False,
            })
            rec["statuses"].append(test["status"])
            rec["runs"].append(run.number)
            rec["allure_flaky"] = rec["allure_flaky"] or test["flaky"]
            if test["status"] != "skipped":
                rec["durations"].append(test["duration"] / MS)
            if test["suite"] != "(no suite)":
                rec["suite"] = test["suite"]

    for rec in by_identity.values():
        statuses = rec["statuses"]
        bad = sum(1 for s in statuses if s in ("failed", "broken"))
        good = sum(1 for s in statuses if s == "passed")
        rec["failures"] = bad
        rec["executions"] = bad + good
        rec["pass_rate"] = pct(good, bad + good)
        rec["avg_duration"] = mean(rec["durations"])
        rec["max_duration"] = max(rec["durations"]) if rec["durations"] else None
        # Flipped between outcomes across the window -> flaky.
        rec["flaky"] = rec["allure_flaky"] or (bad > 0 and good > 0)
        rec["always_skipped"] = bool(statuses) and all(s == "skipped" for s in statuses)
    return by_identity


def aggregate_suites(runs):
    """Suite cards from the latest run, enriched with window-wide averages."""
    latest = runs[-1]
    cards = []
    durations = {}
    for test in latest.tests:
        durations.setdefault(test["suite"], []).append(test["duration"] / MS)

    for item in latest.suites:
        stat = item.get("statistic") or {}
        total = int(stat.get("total") or 0)
        skipped = int(stat.get("skipped") or 0)
        passed = int(stat.get("passed") or 0)
        failed = int(stat.get("failed") or 0)
        broken = int(stat.get("broken") or 0)
        executed = max(total - skipped, 0)
        name = item.get("name") or "(unnamed)"
        cards.append({
            "name": name,
            "scope": "API" if name.startswith("api.") else ("UI" if name.startswith("ui.") else "Other"),
            "total": total,
            "passed": passed,
            "failed": failed,
            "broken": broken,
            "skipped": skipped,
            "executed": executed,
            "pass_rate": pct(passed, executed),
            "avg_duration": mean([d for d in durations.get(name, []) if d > 0]),
        })
    cards.sort(key=lambda c: ((c["pass_rate"] if c["pass_rate"] is not None else 999), -c["total"]))
    return cards


def scope_of(test):
    suite = test.get("suite") or ""
    full = test.get("full_name") or ""
    if suite.startswith("api.") or full.startswith("api."):
        return "API"
    if suite.startswith("ui.") or full.startswith("ui."):
        return "UI"
    return "Other"


def build_metrics(runs, gates, coverage):
    """Headline metrics: latest run for counts, window mean for rates."""
    latest = runs[-1]
    window = {
        "pass_rate": mean([r.pass_rate for r in runs]),
        "fail_rate": mean([r.fail_rate for r in runs]),
        "broken_rate": mean([r.broken_rate for r in runs]),
        "skip_rate": mean([r.skip_rate for r in runs]),
        "test_coverage": mean([r.test_coverage for r in runs]),
        "avg_duration_sec": mean([r.avg_duration for r in runs]),
        "suite_duration_sec": mean([r.wall_seconds for r in runs]),
    }

    tests = aggregate_tests(runs)
    tracked = [t for t in tests.values() if t["executions"] > 0]
    flaky = [t for t in tracked if t["flaky"]]
    flaky_rate = pct(len(flaky), len(tracked))

    api_durations = [t["duration"] / MS for t in latest.tests
                     if t["status"] != "skipped" and scope_of(t) == "API"]
    ui_durations = [t["duration"] / MS for t in latest.tests
                    if t["status"] != "skipped" and scope_of(t) == "UI"]

    metrics = {
        # --- the eleven requested headline metrics ---------------------------------
        "pass_rate": latest.pass_rate,
        "total_tests": latest.total,
        "avg_duration_sec": latest.avg_duration,
        "critical_failures": latest.critical_failures,
        "skipped_tests": latest.skipped,
        "test_coverage": latest.test_coverage,
        "api_coverage": coverage["api_coverage"] if coverage else None,
        "conditions_coverage": coverage["conditions_coverage"] if coverage else None,
        "full_coverage": coverage["full"] if coverage else None,
        "partial_coverage": coverage["partial"] if coverage else None,
        "empty_coverage": coverage["empty"] if coverage else None,
        # --- supporting ------------------------------------------------------------
        "full_coverage_rate": coverage["full_coverage_rate"] if coverage else None,
        "fail_rate": latest.fail_rate,
        "broken_rate": latest.broken_rate,
        "skip_rate": latest.skip_rate,
        "flaky_rate": flaky_rate,
        "flaky_count": len(flaky),
        "suite_duration_sec": latest.wall_seconds,
        "passed_tests": latest.passed,
        "failed_tests": latest.failed,
        "broken_tests": latest.broken,
        "executed_tests": latest.executed,
        "avg_api_duration_sec": mean(api_durations),
        "avg_ui_duration_sec": mean(ui_durations),
        "p95_duration_sec": percentile([t["duration"] / MS for t in latest.tests
                                        if t["status"] != "skipped"], 95),
        "run_number": latest.number,
        "runs_in_window": len(runs),
        "window": window,
    }
    metrics["gates"] = evaluate_gates(metrics, gates)
    metrics["_tests"] = tests
    return metrics


def evaluate_gates(metrics, gates):
    """Three-state evaluation against the configured targets."""
    results = []
    for key, gate in gates.items():
        if key.startswith("_"):
            continue
        actual = metrics.get(key)
        # Rate-style gates are judged on the window average, not a single run.
        if key in metrics.get("window", {}) and metrics["window"].get(key) is not None:
            actual = metrics["window"][key]
        if actual is None:
            state = "none"
        else:
            target, warn = float(gate["value"]), float(gate.get("warn", gate["value"]))
            if gate["direction"] == "minimum":
                state = "good" if actual >= warn else ("warn" if actual >= target else "bad")
            else:
                state = "good" if actual <= warn else ("warn" if actual <= target else "bad")
        results.append({
            "key": key,
            "name": gate.get("name", key),
            "actual": actual,
            "target": gate["value"],
            "warn": gate.get("warn"),
            "direction": gate["direction"],
            "unit": gate.get("unit", "percent"),
            "state": state,
            "recommendation": gate.get("recommendation", ""),
        })
    order = {"bad": 0, "warn": 1, "good": 2, "none": 3}
    results.sort(key=lambda r: (order[r["state"]], r["name"]))
    return results


def format_gate_value(value, unit):
    if value is None:
        return "–"
    if unit == "percent":
        return fmt_pct(value)
    if unit == "seconds":
        return fmt_secs(value)
    return fmt_int(int(value))


def validate_gates(gates):
    """Reject a broken config loudly rather than rendering nonsense."""
    errors = []
    for key, gate in gates.items():
        if key.startswith("_"):
            continue
        if not isinstance(gate, dict):
            errors.append(f"{key}: not an object")
            continue
        if gate.get("direction") not in ("minimum", "maximum"):
            errors.append(f"{key}: direction must be 'minimum' or 'maximum'")
        for field in ("value", "warn"):
            if field not in gate:
                continue
            value = gate[field]
            if not isinstance(value, (int, float)) or isinstance(value, bool) or value < 0:
                errors.append(f"{key}.{field}: must be a non-negative number")
            elif gate.get("unit", "percent") == "percent" and value > 100:
                errors.append(f"{key}.{field}: percentage target above 100")
        if "value" not in gate:
            errors.append(f"{key}: missing 'value'")
    if errors:
        raise SystemExit("Invalid quality-gates config:\n  " + "\n  ".join(errors))
    return gates


# --------------------------------------------------------------------------------------
# SVG chart primitives
#
# Mark specs (fixed): bars <=24px thick with a 4px rounded data-end and a square
# baseline; lines 2px; markers r>=4 with a 2px surface ring; area fill ~10%; gridlines
# hairline 1px SOLID (never dashed); a 2px surface gap between touching marks.
# --------------------------------------------------------------------------------------
def rounded_top_path(x, y, w, h, r):
    """Rect with rounded top corners and a square baseline - the bar 'data-end'."""
    r = max(0.0, min(r, w / 2.0, h))
    if h <= 0:
        return ""
    return (f"M{x:.2f},{y + h:.2f} L{x:.2f},{y + r:.2f} Q{x:.2f},{y:.2f} {x + r:.2f},{y:.2f} "
            f"L{x + w - r:.2f},{y:.2f} Q{x + w:.2f},{y:.2f} {x + w:.2f},{y + r:.2f} "
            f"L{x + w:.2f},{y + h:.2f} Z")


def rounded_right_path(x, y, w, h, r):
    """Rect with rounded right corners - a horizontal bar's data-end."""
    r = max(0.0, min(r, h / 2.0, w))
    if w <= 0:
        return ""
    return (f"M{x:.2f},{y:.2f} L{x + w - r:.2f},{y:.2f} Q{x + w:.2f},{y:.2f} {x + w:.2f},{y + r:.2f} "
            f"L{x + w:.2f},{y + h - r:.2f} Q{x + w:.2f},{y + h:.2f} {x + w - r:.2f},{y + h:.2f} "
            f"L{x:.2f},{y + h:.2f} Z")


def nice_ticks(lo, hi, count=4):
    """Clean, round tick values spanning [lo, hi]."""
    if hi <= lo:
        hi = lo + 1
    raw = (hi - lo) / max(count, 1)
    magnitude = 10 ** math.floor(math.log10(raw)) if raw > 0 else 1
    for mult in (1, 2, 2.5, 5, 10):
        step = magnitude * mult
        if step >= raw:
            break
    start = math.floor(lo / step) * step
    ticks = []
    value = start
    while value <= hi + step * 0.001:
        if value >= lo - step * 0.001:
            ticks.append(round(value, 6))
        value += step
    return ticks or [lo, hi]


def svg_line_chart(series, labels, chart_id, y_label="", target=None, target_label="",
                   height=210, unit="%", domain=None):
    """Single-series trend line with an area wash, end-dot and a solid target annotation.

    One series, so no legend box - the card title names what is plotted.
    """
    pts = [(i, v) for i, v in enumerate(series) if v is not None]
    if len(pts) < 1:
        return '<p class="empty">Not enough data yet.</p>'

    pad_l, pad_r, pad_t, pad_b = 46, 16, 14, 30
    width = 720
    plot_w = width - pad_l - pad_r
    plot_h = height - pad_t - pad_b

    values = [v for _, v in pts] + ([target] if target is not None else [])
    lo, hi = min(values), max(values)
    if domain:
        lo, hi = min(lo, domain[0]), max(hi, domain[1])
    span = hi - lo
    if span < 1e-9:
        lo, hi = lo - 1, hi + 1
    else:
        lo -= span * 0.18
        hi += span * 0.18
    if unit == "%":
        hi = min(hi, 100.5)
        lo = max(lo, 0.0)

    ticks = nice_ticks(lo, hi, 4)
    lo, hi = min(lo, ticks[0]), max(hi, ticks[-1])

    def sx(i):
        return pad_l + (plot_w * i / max(len(series) - 1, 1))

    def sy(v):
        return pad_t + plot_h * (1 - (v - lo) / (hi - lo))

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" data-chart="{e(chart_id)}" '
           f'aria-label="{e(y_label or chart_id)} trend across {len(series)} runs">']

    # Gridlines + y ticks - hairline, solid, recessive.
    for t in ticks:
        y = sy(t)
        out.append(f'<line x1="{pad_l}" y1="{y:.2f}" x2="{width - pad_r}" y2="{y:.2f}" class="grid"/>')
        out.append(f'<text x="{pad_l - 8}" y="{y + 3.5:.2f}" class="tick tick-y">'
                   f'{t:g}{"%" if unit == "%" else ""}</text>')

    # Target annotation: solid (dashed rules are an anti-pattern), colored + labeled.
    if target is not None and lo <= target <= hi:
        y = sy(target)
        out.append(f'<line x1="{pad_l}" y1="{y:.2f}" x2="{width - pad_r}" y2="{y:.2f}" class="targetline"/>')
        out.append(f'<text x="{width - pad_r}" y="{y - 6:.2f}" class="targetlabel" '
                   f'text-anchor="end">{e(target_label)}</text>')

    # Area wash at ~10%, then the 2px line.
    path = " ".join(f'{"M" if k == 0 else "L"}{sx(i):.2f},{sy(v):.2f}' for k, (i, v) in enumerate(pts))
    baseline = pad_t + plot_h
    out.append(f'<path d="{path} L{sx(pts[-1][0]):.2f},{baseline:.2f} L{sx(pts[0][0]):.2f},{baseline:.2f} Z" '
               f'class="area"/>')
    out.append(f'<path d="{path}" class="line"/>')

    # Hover dots: r=4 (>=8px mark) with a 2px surface ring; hit target is larger.
    for i, v in pts:
        label = labels[i] if i < len(labels) else str(i)
        value_txt = f"{v:.1f}{unit}" if unit == "%" else f"{v:.1f}"
        out.append(f'<circle cx="{sx(i):.2f}" cy="{sy(v):.2f}" r="4" class="dot"/>')
        out.append(f'<circle cx="{sx(i):.2f}" cy="{sy(v):.2f}" r="13" class="hit" '
                   f'data-tip="Run #{e(label)} — {e(value_txt)}"/>')

    # Direct-label the endpoint only (never a number on every point).
    last_i, last_v = pts[-1]
    end_txt = f"{last_v:.1f}{unit}" if unit == "%" else f"{last_v:.1f}"
    anchor_x = sx(last_i)
    out.append(f'<text x="{anchor_x - 10:.2f}" y="{sy(last_v) - 12:.2f}" class="endlabel" '
               f'text-anchor="end">{e(end_txt)}</text>')

    # X ticks: first, last and a few between, so labels never collide.
    step = max(1, len(series) // 7)
    for i in range(len(series)):
        if i % step and i != len(series) - 1:
            continue
        out.append(f'<text x="{sx(i):.2f}" y="{height - 10}" class="tick" '
                   f'text-anchor="middle">{e(labels[i] if i < len(labels) else i)}</text>')

    out.append("</svg>")
    return "".join(out)


def svg_status_columns(runs, height=230):
    """Stacked columns of test outcomes per run.

    Stack order passed -> skipped -> failed -> broken is the CVD-validated adjacency
    order. Segments are separated by a 2px surface gap, never a stroke.
    """
    if not runs:
        return '<p class="empty">No runs.</p>'

    pad_l, pad_r, pad_t, pad_b = 46, 16, 14, 30
    width = 720
    plot_w = width - pad_l - pad_r
    plot_h = height - pad_t - pad_b
    max_total = max((r.total for r in runs), default=0) or 1

    ticks = nice_ticks(0, max_total, 4)
    top = max(max_total, ticks[-1])
    band = plot_w / len(runs)
    bar_w = min(24.0, band * 0.62)          # cap thickness; leftover band is air
    gap = 2.0                                # the surface gap

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" '
           f'aria-label="Test outcomes per run, stacked by status">']

    for t in ticks:
        y = pad_t + plot_h * (1 - t / top)
        out.append(f'<line x1="{pad_l}" y1="{y:.2f}" x2="{width - pad_r}" y2="{y:.2f}" class="grid"/>')
        out.append(f'<text x="{pad_l - 8}" y="{y + 3.5:.2f}" class="tick tick-y">{t:g}</text>')

    baseline = pad_t + plot_h
    for idx, run in enumerate(runs):
        x = pad_l + band * idx + (band - bar_w) / 2
        cursor = baseline
        segments = [(s, getattr(run, s)) for s in STATUS_ORDER]
        drawn = [(s, c) for s, c in segments if c > 0]
        for order, (status, count) in enumerate(drawn):
            raw_h = plot_h * count / top
            is_top = order == len(drawn) - 1
            seg_h = raw_h if is_top else max(raw_h - gap, 0.6)
            y = cursor - raw_h
            tip = (f"Run #{run.number} — {count} {status} "
                   f"({pct(count, run.total):.0f}% of {run.total})")
            if is_top:
                out.append(f'<path d="{rounded_top_path(x, y, bar_w, seg_h, 4)}" '
                           f'fill="var(--st-{status})" data-tip="{e(tip)}" class="mark"/>')
            else:
                out.append(f'<rect x="{x:.2f}" y="{y + (raw_h - seg_h):.2f}" width="{bar_w:.2f}" '
                           f'height="{seg_h:.2f}" fill="var(--st-{status})" '
                           f'data-tip="{e(tip)}" class="mark"/>')
            cursor -= raw_h

    step = max(1, len(runs) // 8)
    for idx, run in enumerate(runs):
        if idx % step and idx != len(runs) - 1:
            continue
        out.append(f'<text x="{pad_l + band * idx + band / 2:.2f}" y="{height - 10}" '
                   f'class="tick" text-anchor="middle">{run.number}</text>')

    out.append("</svg>")
    return "".join(out)


def svg_stacked_bar(segs, total, aria_label, noun, light_ink_keys=(), height=92):
    """One horizontal stacked bar for a part-to-whole split on an *ordered* scale.

    Shared by API operation coverage and UI page coverage: both grade the same way
    (best -> worst), so both use the ordinal one-hue ramp rather than categorical hues.

    segs           (key, label, count, css_color) in best-to-worst order
    light_ink_keys segment keys whose fill is pale enough to need dark ink
    """
    pad_l, pad_r = 4, 4
    width = 720
    bar_w = width - pad_l - pad_r
    bar_h = 34
    y = 16
    gap = 2.0

    drawn = [s for s in segs if s[2] > 0]

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" '
           f'aria-label="{e(aria_label)}">']
    cursor = float(pad_l)
    for order, (key, label, count, color) in enumerate(drawn):
        raw_w = bar_w * count / total
        is_end = order == len(drawn) - 1
        seg_w = raw_w if is_end else max(raw_w - gap, 0.6)
        share = pct(count, total)
        tip = f"{label}: {count} of {total} {noun} ({share:.1f}%)"
        if is_end:
            out.append(f'<path d="{rounded_right_path(cursor, y, seg_w, bar_h, 4)}" fill="{color}" '
                       f'data-tip="{e(tip)}" class="mark"/>')
        else:
            out.append(f'<rect x="{cursor:.2f}" y="{y}" width="{seg_w:.2f}" height="{bar_h}" '
                       f'fill="{color}" data-tip="{e(tip)}" class="mark"/>')
        # Only label inside when the text comfortably fits (never clip, never overflow).
        text = f"{count}"
        if seg_w >= 34:
            ink = "var(--on-fill-light)" if key in light_ink_keys else "var(--on-fill-dark)"
            out.append(f'<text x="{cursor + seg_w / 2:.2f}" y="{y + bar_h / 2 + 4.5:.2f}" '
                       f'class="segval" fill="{ink}" text-anchor="middle">{e(text)}</text>')
        out.append(f'<text x="{cursor:.2f}" y="{y + bar_h + 18:.2f}" class="tick">'
                   f'{e(label)} {share:.0f}%</text>')
        cursor += raw_w
    out.append("</svg>")
    return "".join(out)


def svg_coverage_bar(coverage, height=92):
    """API operations: full / partial / empty."""
    if not coverage or not coverage["operations_all"]:
        return '<p class="empty">No swagger-coverage results published for this run.</p>'
    return svg_stacked_bar(
        [("full", "Full", coverage["full"], "var(--cov-full)"),
         ("partial", "Partial", coverage["partial"], "var(--cov-partial)"),
         ("empty", "Empty", coverage["empty"], "var(--cov-empty)")],
        coverage["operations_all"],
        "API operation coverage: full, partial and empty",
        "operations", light_ink_keys=("empty",), height=height)


def svg_ui_coverage_bar(ui, height=92):
    """UI page objects: opened by URL / reached by transition / untouched.

    Same ordered grading as API coverage (best -> worst), so it reuses the same
    validated one-hue ramp instead of introducing a second colour language.
    """
    if not ui or not ui["pages_total"]:
        return '<p class="empty">No UI route-coverage results for this run.</p>'
    return svg_stacked_bar(
        [("opened", "Opened", ui["pages_opened"], "var(--cov-full)"),
         ("reached", "Reached", ui["pages_reached"], "var(--cov-partial)"),
         ("untouched", "Untouched", ui["pages_untouched_count"], "var(--cov-empty)")],
        ui["pages_total"],
        "UI page coverage: opened, reached and untouched",
        "page objects", light_ink_keys=("untouched",), height=height)


def svg_hbars(items, height_per=26, unit="s", max_items=8):
    """Horizontal bars for magnitude (slowest tests). Sequential single hue."""
    items = [i for i in items if i[1] is not None][:max_items]
    if not items:
        return '<p class="empty">No timing data.</p>'

    width = 720
    label_w = 300
    pad_r = 62
    bar_h = 14                                   # thin marks
    row_h = height_per
    height = row_h * len(items) + 12
    plot_w = width - label_w - pad_r
    top = max(v for _, v in items) or 1

    out = [f'<svg class="chart" viewBox="0 0 {width} {height}" role="img" '
           f'preserveAspectRatio="xMidYMid meet" aria-label="Slowest tests by average duration">']
    for idx, (name, value) in enumerate(items):
        y = 6 + idx * row_h
        w = plot_w * value / top
        out.append(f'<text x="0" y="{y + bar_h / 2 + 4:.2f}" class="rowlabel">{e(short_name(name, 42))}</text>')
        out.append(f'<path d="{rounded_right_path(label_w, y, max(w, 1), bar_h, 4)}" '
                   f'fill="var(--accent)" class="mark" '
                   f'data-tip="{e(short_name(name, 70))} — {value:.1f}{unit}"/>')
        out.append(f'<text x="{label_w + w + 8:.2f}" y="{y + bar_h / 2 + 4:.2f}" class="barval">'
                   f'{value:.1f}{unit}</text>')
    out.append("</svg>")
    return "".join(out)


def svg_sparkline(values, width=104, height=28):
    """12-point sparkline for a stat tile: de-emphasis hue, current point in the accent."""
    pts = [(i, v) for i, v in enumerate(values) if v is not None]
    if len(pts) < 2:
        return ""
    vals = [v for _, v in pts]
    lo, hi = min(vals), max(vals)
    if hi - lo < 1e-9:
        lo, hi = lo - 1, hi + 1
    n = max(len(values) - 1, 1)

    def sx(i):
        return 2 + (width - 4) * i / n

    def sy(v):
        return 3 + (height - 6) * (1 - (v - lo) / (hi - lo))

    path = " ".join(f'{"M" if k == 0 else "L"}{sx(i):.1f},{sy(v):.1f}' for k, (i, v) in enumerate(pts))
    last_i, last_v = pts[-1]
    return (f'<svg class="spark" viewBox="0 0 {width} {height}" aria-hidden="true">'
            f'<path d="{path}" class="sparkline"/>'
            f'<circle cx="{sx(last_i):.1f}" cy="{sy(last_v):.1f}" r="2.6" class="sparkdot"/></svg>')


# --------------------------------------------------------------------------------------
# Page rendering
# --------------------------------------------------------------------------------------
CSS = """
*,*::before,*::after{box-sizing:border-box}
:root{
  color-scheme:light;
  --surface-0:#f4f3f0; --surface-1:#fcfcfb; --border:#e2e0da;
  --text-primary:#0b0b0b; --text-secondary:#52514e; --text-muted:#83817a;
  --accent:#2a78d6; --deemph:#c9c7c0;
  --grid:#e8e6e0;
  --cov-full:#1c5cab; --cov-partial:#3987e5; --cov-empty:#86b6ef;
  --on-fill-dark:#ffffff; --on-fill-light:#0b0b0b;
  --st-passed:#0ca30c; --st-skipped:#fab219; --st-failed:#d03b3b; --st-broken:#ec835a;
  --good:#0ca30c; --warn:#fab219; --bad:#d03b3b;
  --good-ink:#0a7a0a; --warn-ink:#8a6100; --bad-ink:#b32f2f;
}
@media (prefers-color-scheme:dark){
  :root:not([data-theme="light"]){
    color-scheme:dark;
    --surface-0:#121211; --surface-1:#1a1a19; --border:#333330;
    --text-primary:#ffffff; --text-secondary:#c3c2b7; --text-muted:#96958c;
    --accent:#3987e5; --deemph:#4a4a46;
    --grid:#2c2c29;
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
  --grid:#2c2c29;
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
h1{font-size:26px;line-height:1.2;margin:0 0 4px;font-weight:650;letter-spacing:-.01em}
h2{font-size:18px;margin:0 0 4px;font-weight:620;letter-spacing:-.005em}
h3{font-size:14px;margin:0 0 10px;font-weight:600;color:var(--text-secondary)}
.sub{color:var(--text-secondary);font-size:13.5px;margin:0}
.muted{color:var(--text-muted)}
header.page{display:flex;flex-wrap:wrap;gap:16px;align-items:flex-start;justify-content:space-between;margin-bottom:22px}
.links{display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.btn{
  display:inline-flex;align-items:center;gap:6px;padding:6px 12px;border-radius:8px;
  border:1px solid var(--border);background:var(--surface-1);color:var(--text-primary);
  font-size:13px;text-decoration:none;cursor:pointer;font-family:inherit
}
.btn:hover{border-color:var(--accent);color:var(--accent)}
.card{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:18px 20px;margin-bottom:18px}
.card>.chart,.card>.scroll>.chart{margin-top:10px}
.chart{width:100%;height:auto;display:block;overflow:visible}
.scroll{overflow-x:auto;max-width:100%}
.grid-kpi{display:grid;grid-template-columns:repeat(auto-fit,minmax(178px,1fr));gap:12px;margin-bottom:18px}
.tile{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:14px 16px;min-width:0}
.tile .label{font-size:12.5px;color:var(--text-secondary);margin-bottom:6px}
.tile .value{font-size:27px;font-weight:640;letter-spacing:-.02em;line-height:1.1;overflow-wrap:anywhere}
.tile .note{font-size:12px;color:var(--text-muted);margin-top:5px}
.tile .spark{margin-top:8px;width:100%;max-width:120px;height:28px;display:block}
.hero{background:var(--surface-1);border:1px solid var(--border);border-radius:12px;padding:22px 24px;margin-bottom:18px;
  display:flex;flex-wrap:wrap;gap:22px;align-items:center;justify-content:space-between}
.hero .figure{font-size:56px;font-weight:660;letter-spacing:-.03em;line-height:1}
.hero .caption{font-size:13.5px;color:var(--text-secondary);margin-top:6px;max-width:52ch}
.hero .aside{display:flex;gap:26px;flex-wrap:wrap}
.hero .aside div{min-width:88px}
.hero .aside .v{font-size:20px;font-weight:620;letter-spacing:-.01em}
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
.pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;padding:2px 8px;border-radius:99px;border:1px solid var(--border)}
.pill.good{color:var(--good-ink)} .pill.warn{color:var(--warn-ink)} .pill.bad{color:var(--bad-ink)} .pill.none{color:var(--text-muted)}
.formula{font-size:12.5px;color:var(--text-muted);margin-top:10px;font-variant-numeric:tabular-nums}
.grid{stroke:var(--grid);stroke-width:1}
.targetline{stroke:var(--text-muted);stroke-width:1}
.targetlabel{fill:var(--text-muted);font-size:11px}
.line{fill:none;stroke:var(--accent);stroke-width:2;stroke-linejoin:round;stroke-linecap:round}
.area{fill:var(--accent);opacity:.10;stroke:none}
.dot{fill:var(--accent);stroke:var(--surface-1);stroke-width:2}
.hit{fill:transparent;cursor:pointer}
.mark{cursor:pointer}
.mark:hover{opacity:.82}
.tick{fill:var(--text-muted);font-size:11px;font-variant-numeric:tabular-nums}
.tick-y{text-anchor:end}
.endlabel{fill:var(--text-primary);font-size:12.5px;font-weight:600;font-variant-numeric:tabular-nums}
.segval{font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}
.rowlabel{fill:var(--text-secondary);font-size:12.5px}
.barval{fill:var(--text-primary);font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}
.sparkline{fill:none;stroke:var(--deemph);stroke-width:1.6;stroke-linejoin:round;stroke-linecap:round}
.sparkdot{fill:var(--accent)}
.empty{color:var(--text-muted);font-size:13px;margin:10px 0 0}
#tip{
  position:fixed;pointer-events:none;opacity:0;transition:opacity .1s;z-index:50;
  background:var(--text-primary);color:var(--surface-1);font-size:12.5px;
  padding:5px 9px;border-radius:6px;max-width:320px;line-height:1.35
}
details.raw{margin-top:12px}
details.raw summary{cursor:pointer;font-size:13px;color:var(--text-secondary)}
footer{margin-top:30px;font-size:12.5px;color:var(--text-muted);line-height:1.7}
@media (max-width:640px){
  .hero .figure{font-size:44px}
  .wrap{padding:18px 14px 48px}
}
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
  function show(ev,text){
    tip.textContent=text; tip.style.opacity='1';
    var r=tip.getBoundingClientRect();
    var x=Math.min(Math.max(ev.clientX+14,8),window.innerWidth-r.width-8);
    var y=ev.clientY-r.height-12; if(y<8){y=ev.clientY+18;}
    tip.style.left=x+'px'; tip.style.top=y+'px';
  }
  document.addEventListener('mouseover',function(ev){
    var t=ev.target.closest('[data-tip]');
    if(t){show(ev,t.getAttribute('data-tip'));}
  });
  document.addEventListener('mousemove',function(ev){
    var t=ev.target.closest('[data-tip]');
    if(t){show(ev,t.getAttribute('data-tip'));}else{tip.style.opacity='0';}
  });
  document.addEventListener('mouseleave',function(){tip.style.opacity='0';});
})();
"""


def tile(label, value, note="", spark_values=None):
    spark = svg_sparkline(spark_values[-12:]) if spark_values else ""
    note_html = f'<div class="note">{e(note)}</div>' if note else ""
    return (f'<div class="tile"><div class="label">{e(label)}</div>'
            f'<div class="value">{value}</div>{note_html}{spark}</div>')


def status_legend():
    parts = []
    for status in STATUS_ORDER:
        parts.append(f'<span><i class="swatch" style="background:var(--st-{status})"></i>'
                     f'{STATUS_ICONS[status]} {status.capitalize()}</span>')
    return f'<div class="legend">{"".join(parts)}</div>'


def render_gates_table(gates):
    rows = []
    for g in gates:
        direction = "≥" if g["direction"] == "minimum" else "≤"
        rows.append(
            f'<tr><td>{e(g["name"])}</td>'
            f'<td class="num">{e(format_gate_value(g["actual"], g["unit"]))}</td>'
            f'<td class="num">{direction} {e(format_gate_value(g["target"], g["unit"]))}</td>'
            f'<td><span class="pill {g["state"]}">{GATE_ICONS[g["state"]]} '
            f'{g["state"].upper() if g["state"] != "none" else "NO DATA"}</span></td>'
            f'<td class="wrapname muted">{e(g["recommendation"])}</td></tr>')
    return ('<div class="scroll"><table><thead><tr><th>Gate</th><th class="num">Actual</th>'
            '<th class="num">Target</th><th>Status</th><th>If it breaches</th></tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table></div>')


def render_runs_table(runs, pages_base=".."):
    rows = []
    for run in reversed(runs):
        # Must go through pages_base: the dashboard is written at two different depths
        # (quality/<slug>/index.html and <run>/quality.html), so a hardcoded "../" is
        # correct for at most one of them.
        link = f'<a href="{e(pages_base)}/{run.number}/index.html">#{run.number}</a>'
        rows.append(
            f'<tr><td>{link}</td>'
            f'<td class="num">{run.total}</td><td class="num">{run.passed}</td>'
            f'<td class="num">{run.failed}</td><td class="num">{run.broken}</td>'
            f'<td class="num">{run.skipped}</td>'
            f'<td class="num">{e(fmt_pct(run.pass_rate))}</td>'
            f'<td class="num">{e(fmt_secs(run.avg_duration))}</td>'
            f'<td class="num">{e(fmt_secs(run.wall_seconds))}</td>'
            f'<td class="muted">{e(run.started_at.strftime("%Y-%m-%d %H:%M") if run.started_at else "–")}</td></tr>')
    return ('<div class="scroll"><table><thead><tr><th>Run</th><th class="num">Total</th>'
            '<th class="num">Passed</th><th class="num">Failed</th><th class="num">Broken</th>'
            '<th class="num">Skipped</th><th class="num">Pass rate</th>'
            '<th class="num">Avg test</th><th class="num">Suite</th><th>Started (UTC)</th>'
            f'</tr></thead><tbody>{"".join(rows)}</tbody></table></div>')


def render_suites_table(cards):
    rows = []
    for c in cards:
        rows.append(
            f'<tr><td class="wrapname">{e(c["name"])}</td><td>{e(c["scope"])}</td>'
            f'<td class="num">{c["total"]}</td><td class="num">{c["passed"]}</td>'
            f'<td class="num">{c["failed"] + c["broken"]}</td><td class="num">{c["skipped"]}</td>'
            f'<td class="num">{e(fmt_pct(c["pass_rate"]))}</td>'
            f'<td class="num">{e(fmt_secs(c["avg_duration"]))}</td></tr>')
    return ('<div class="scroll"><table><thead><tr><th>Suite</th><th>Scope</th>'
            '<th class="num">Tests</th><th class="num">Passed</th><th class="num">Failed</th>'
            '<th class="num">Skipped</th><th class="num">Pass rate</th><th class="num">Avg</th>'
            f'</tr></thead><tbody>{"".join(rows)}</tbody></table></div>')


def render_attention_table(tests, runs):
    """Tests that failed more than once, flipped status, or never run in the window."""
    window = len(runs)
    rows = []
    for rec in tests.values():
        reasons = []
        if rec["flaky"]:
            reasons.append("flaky (status flips across runs)")
        if rec["failures"] > 1:
            reasons.append(f'failed {rec["failures"]}×')
        if rec["always_skipped"]:
            reasons.append(f"skipped in all {window} runs")
        if not reasons:
            continue
        rows.append((
            -rec["failures"], rec["name"],
            f'<tr><td class="wrapname">{e(short_name(rec["name"], 60))}</td>'
            f'<td class="muted">{e(rec["suite"])}</td>'
            f'<td class="num">{rec["failures"]}</td>'
            f'<td class="num">{rec["executions"]}</td>'
            f'<td class="num">{e(fmt_pct(rec["pass_rate"]))}</td>'
            f'<td class="muted wrapname">{e(", ".join(reasons))}</td></tr>'))
    if not rows:
        return '<p class="empty">Nothing needs attention in this window — no repeat failures, flips or permanently skipped tests.</p>'
    rows.sort()
    return ('<div class="scroll"><table><thead><tr><th>Test</th><th>Suite</th>'
            '<th class="num">Failures</th><th class="num">Executions</th>'
            '<th class="num">Pass rate</th><th>Why</th></tr></thead>'
            f'<tbody>{"".join(r[2] for r in rows[:25])}</tbody></table></div>')


def render_predicates_table(coverage):
    rows = []
    for p in coverage["predicates"]:
        rows.append(f'<tr><td>{e(p["name"])}</td><td class="num">{p["covered"]}</td>'
                    f'<td class="num">{p["all"]}</td>'
                    f'<td class="num">{e(fmt_pct(p["rate"]))}</td></tr>')
    return ('<div class="scroll"><table><thead><tr><th>Condition type</th>'
            '<th class="num">Covered</th><th class="num">All</th><th class="num">Rate</th>'
            f'</tr></thead><tbody>{"".join(rows)}</tbody></table></div>')


UI_STATUS_META = {
    "opened":    ("cov-full",    "◉", "Opened"),
    "reached":   ("cov-partial", "◐", "Reached"),
    "untouched": ("cov-empty",   "○", "Untouched"),
}


def render_ui_pages_table(ui):
    """Page objects, worst grade first — the untouched ones are the actionable rows."""
    order = {"untouched": 0, "reached": 1, "opened": 2}
    rows = []
    for page in sorted(ui["pages"], key=lambda p: (order.get(p.get("status"), 9),
                                                   p.get("name", ""))):
        status = page.get("status", "untouched")
        var, icon, label = UI_STATUS_META.get(status, UI_STATUS_META["untouched"])
        route = page.get("route") or "–"
        by = page.get("reached_by") or []
        # Test paths are long; show the class names and keep the full list in the title.
        names = [os.path.basename(p).replace(".java", "") for p in by]
        shown = ", ".join(names[:2]) + (f" +{len(names) - 2}" if len(names) > 2 else "")
        rows.append(
            f'<tr><td>{e(page.get("name", "?"))}</td>'
            f'<td class="muted"><code>{e(route)}</code></td>'
            f'<td><span class="pill" style="background:var(--{var});color:var(--on-fill-'
            f'{"light" if status == "untouched" else "dark"})">{icon} {label}</span></td>'
            f'<td class="muted" title="{e(", ".join(by))}">{e(shown or "–")}</td></tr>')
    return ('<div class="scroll"><table><thead><tr><th>Page object</th><th>Route</th>'
            '<th>Status</th><th>Reached by</th></tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table></div>')


def render_ui_coverage_section(ui):
    if not ui:
        return """
<section class="card">
  <h2>UI coverage</h2>
  <p class="empty">No UI route-coverage results for this run. The CI job must run
     <code>scripts/build_route_coverage.py --json</code> and pass the file to
     <code>--ui-coverage</code> for page and route coverage to appear here.</p>
</section>"""

    gaps = []
    stranded = ui["pages_untouched"] + ui["pages_untouched_base_classes"]
    if stranded:
        gaps.append(f'<p class="formula"><strong>Untouched pages:</strong> '
                    f'{e(", ".join(sorted(stranded)))}</p>')
    if ui["routes_missing"]:
        # Escape each route, then join with markup — escaping the joined string would
        # turn the separating tags into visible text.
        routes = ", ".join(f"<code>{e(r)}</code>" for r in ui["routes_missing"])
        gaps.append(f'<p class="formula"><strong>Routes never opened:</strong> {routes}</p>')
    if ui["pages_without_route"]:
        gaps.append(f'<p class="formula"><strong>No URL of their own</strong> (wizard steps and '
                    f'overlays, reachable only by transition): '
                    f'{e(", ".join(sorted(ui["pages_without_route"])))}</p>')

    return f"""
<section class="card">
  <h2>UI coverage</h2>
  <p class="sub">There is no swagger.json for a UI, so the denominator is derived statically
     from the page objects under <code>src/main/java/ui/pages</code> and the URL templates their
     <code>url()</code> methods declare. Measured across {ui["test_cases"]} UI test cases in
     {ui["test_classes"]} classes. Ordered scale, so one hue: darker means better covered.</p>
  <div class="scroll">{svg_ui_coverage_bar(ui)}</div>
  <div class="legend">
    <span><i class="swatch" style="background:var(--cov-full)"></i>◉ Opened — a test navigates to it by URL</span>
    <span><i class="swatch" style="background:var(--cov-partial)"></i>◐ Reached — landed on by transition only</span>
    <span><i class="swatch" style="background:var(--cov-empty)"></i>○ Untouched — no test reaches it</span>
  </div>
  <p class="formula">Page coverage = (opened + reached) ÷ all page objects =
     ({ui["pages_opened"]} + {ui["pages_reached"]}) ÷ {ui["pages_total"]} =
     {e(fmt_pct(ui["page_coverage"]))}</p>
  <p class="formula">Route coverage = routes opened ÷ distinct <code>url()</code> templates =
     {ui["routes_hit"]} ÷ {ui["routes_total"]} = {e(fmt_pct(ui["route_coverage"]))}</p>
  <h3 style="margin-top:20px">Pages</h3>
  {render_ui_pages_table(ui)}
  {"".join(gaps)}
</section>"""


def render_page(runs, metrics, coverage, cov_series, context, ui=None):
    latest = runs[-1]
    labels = [str(r.number) for r in runs]
    tests = metrics["_tests"]
    suites = aggregate_suites(runs)
    gate_by_key = {g["key"]: g for g in metrics["gates"]}

    def gate_note(key, extra=""):
        g = gate_by_key.get(key)
        if not g or g["state"] == "none":
            return extra
        direction = "≥" if g["direction"] == "minimum" else "≤"
        target = format_gate_value(g["target"], g["unit"])
        mark = GATE_ICONS[g["state"]]
        return f'{mark} target {direction} {target}' + (f" · {extra}" if extra else "")

    breaches = [g for g in metrics["gates"] if g["state"] == "bad"]
    warns = [g for g in metrics["gates"] if g["state"] == "warn"]

    owner_repo = context.get("repo") or ""
    pages_base = context.get("pages_base") or ".."
    header_links = [
        f'<a class="btn" href="{e(pages_base)}/{latest.number}/index.html">Allure report #{latest.number}</a>',
        f'<a class="btn" href="{e(pages_base)}/{latest.number}/swagger-coverage-report.html">Swagger coverage</a>',
    ]
    if latest.build_url:
        header_links.append(f'<a class="btn" href="{e(latest.build_url)}">CI run &amp; logs</a>')
    if owner_repo:
        header_links.append(f'<a class="btn" href="https://github.com/{e(owner_repo)}">Repository</a>')
    header_links.append('<button class="btn" id="theme" type="button">◐ Theme</button>')

    generated = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    # ---- hero + KPI tiles -------------------------------------------------------------
    hero_state = gate_by_key.get("pass_rate", {}).get("state", "none")
    hero = f"""
<section class="hero">
  <div>
    <div class="label muted" style="font-size:12.5px">Pass rate — run #{latest.number}</div>
    <div class="figure">{e(fmt_pct(metrics["pass_rate"]))}</div>
    <div class="caption">{latest.passed} of {latest.executed} executed tests passed.
      Skipped tests are excluded from the denominator — see Test coverage for how much of the
      suite actually ran.</div>
  </div>
  <div class="aside">
    <div><div class="v">{e(fmt_pct(metrics["window"]["pass_rate"]))}</div>
         <div class="k">{len(runs)}-run average</div></div>
    <div><div class="v"><span class="pill {hero_state}">{GATE_ICONS[hero_state]} {hero_state.upper()}</span></div>
         <div class="k">Gate status</div></div>
    <div><div class="v">{len(breaches)} / {len(warns)}</div>
         <div class="k">Gates failing / warning</div></div>
  </div>
</section>"""

    tiles_results = "".join([
        tile("Total tests", fmt_int(metrics["total_tests"]),
             f'{metrics["executed_tests"]} executed · {metrics["skipped_tests"]} skipped',
             [r.total for r in runs]),
        tile("Average duration", fmt_secs(metrics["avg_duration_sec"]),
             gate_note("avg_duration_sec", f'p95 {fmt_secs(metrics["p95_duration_sec"])}'),
             [r.avg_duration for r in runs]),
        tile("Critical failures", fmt_int(metrics["critical_failures"]),
             gate_note("critical_failures", "failed + broken"),
             [r.critical_failures for r in runs]),
        tile("Skipped tests", fmt_int(metrics["skipped_tests"]),
             gate_note("skip_rate", f'{fmt_pct(metrics["skip_rate"])} of suite'),
             [r.skipped for r in runs]),
        tile("Test coverage", fmt_pct(metrics["test_coverage"]),
             gate_note("test_coverage", "executed ÷ total"),
             [r.test_coverage for r in runs]),
    ])

    if coverage:
        tiles_coverage = "".join([
            tile("API coverage", fmt_pct(metrics["api_coverage"]),
                 gate_note("api_coverage",
                           f'{coverage["full"] + coverage["partial"]} of {coverage["operations_all"]} operations'),
                 cov_series.get("api_coverage")),
            tile("Conditions coverage", fmt_pct(metrics["conditions_coverage"]),
                 gate_note("conditions_coverage",
                           f'{coverage["conditions_covered"]} of {coverage["conditions_all"]} conditions'),
                 cov_series.get("conditions_coverage")),
            tile("Full coverage", fmt_int(metrics["full_coverage"]),
                 f'{fmt_pct(coverage["full_coverage_rate"])} of operations — every condition met'),
            tile("Partial coverage", fmt_int(metrics["partial_coverage"]),
                 f'{fmt_pct(coverage["partial_coverage_rate"])} of operations — called, not exhaustively'),
            tile("Empty coverage", fmt_int(metrics["empty_coverage"]),
                 f'{fmt_pct(coverage["empty_coverage_rate"])} of operations — never called'),
        ])
        coverage_section = f"""
<section class="card">
  <h2>API coverage</h2>
  <p class="sub">Operation coverage from swagger-coverage against the filtered TeamCity spec.
     Ordered scale, so one hue: darker means better covered.</p>
  <div class="scroll">{svg_coverage_bar(coverage)}</div>
  <div class="legend">
    <span><i class="swatch" style="background:var(--cov-full)"></i>Full — every declared condition met</span>
    <span><i class="swatch" style="background:var(--cov-partial)"></i>Partial — called, some conditions unmet</span>
    <span><i class="swatch" style="background:var(--cov-empty)"></i>Empty — never called</span>
  </div>
  <p class="formula">API coverage = (full + partial) ÷ all operations =
     ({coverage["full"]} + {coverage["partial"]}) ÷ {coverage["operations_all"]} =
     {e(fmt_pct(coverage["api_coverage"]))}</p>
  <h3 style="margin-top:20px">Conditions by type</h3>
  {render_predicates_table(coverage)}
  <p class="formula">Conditions coverage = covered ÷ all =
     {coverage["conditions_covered"]} ÷ {coverage["conditions_all"]} =
     {e(fmt_pct(coverage["conditions_coverage"]))}</p>
  {render_uncovered(coverage)}
</section>"""
    else:
        tiles_coverage = "".join([
            tile("API coverage", "–", "No swagger-coverage-results.json published"),
            tile("Conditions coverage", "–", "Publish the JSON alongside the HTML report"),
            tile("Full coverage", "–", ""),
            tile("Partial coverage", "–", ""),
            tile("Empty coverage", "–", ""),
        ])
        coverage_section = """
<section class="card">
  <h2>API coverage</h2>
  <p class="empty">No <code>swagger-coverage-results.json</code> found for this run. The CI job
     must copy it next to <code>swagger-coverage-report.html</code> for coverage to be trended here.</p>
</section>"""

    # ---- UI coverage tiles --------------------------------------------------------------
    # Independent of the API coverage branch above: the two measure different stacks and
    # either can be present without the other.
    tiles_ui = ""
    if ui:
        tiles_ui = "".join([
            tile("UI page coverage", fmt_pct(ui["page_coverage"]),
                 f'{ui["pages_opened"] + ui["pages_reached"]} of {ui["pages_total"]} page objects reached'),
            tile("UI route coverage", fmt_pct(ui["route_coverage"]),
                 f'{ui["routes_hit"]} of {ui["routes_total"]} url() templates opened'),
            tile("Opened by URL", fmt_int(ui["pages_opened"]),
                 "a test navigates straight to the page"),
            tile("Reached by transition", fmt_int(ui["pages_reached"]),
                 "landed on via getPage() or a navigation return type"),
            tile("Untouched pages", fmt_int(ui["pages_untouched_count"]),
                 "no UI test reaches these at all"),
        ])

    # ---- coverage trend ---------------------------------------------------------------
    cov_trend = ""
    api_series = cov_series.get("api_coverage") or []
    if len([v for v in api_series if v is not None]) >= 2:
        cov_trend = f"""
  <h3 style="margin-top:22px">API coverage over time</h3>
  <div class="scroll">{svg_line_chart(api_series, cov_series["labels"], "cov",
                                      y_label="API coverage",
                                      target=gate_by_key.get("api_coverage", {}).get("target"),
                                      target_label="target")}</div>"""

    slowest = sorted(
        ((rec["name"], rec["avg_duration"]) for rec in tests.values() if rec["avg_duration"]),
        key=lambda x: -x[1])

    body = f"""
<div class="wrap">
<header class="page">
  <div>
    <h1>Quality Report</h1>
    <p class="sub">{e(owner_repo or "TeamCity test suite")} · run #{latest.number} ·
       window of {len(runs)} runs · generated {e(generated)}</p>
  </div>
  <div class="links">{"".join(header_links)}</div>
</header>

{hero}

<div class="grid-kpi">{tiles_results}</div>
<div class="grid-kpi">{tiles_coverage}</div>

<section class="card">
  <h2>Quality gates</h2>
  <p class="sub">Rates are judged on the {len(runs)}-run average; counts on the latest run.
     Targets live in <code>quality-gates.json</code>.</p>
  {render_gates_table(metrics["gates"])}
</section>

<section class="card">
  <h2>Test results</h2>
  <p class="sub">Outcome of every test, run by run.</p>
  <div class="scroll">{svg_status_columns(runs)}</div>
  {status_legend()}
  <h3 style="margin-top:22px">Pass rate over time</h3>
  <div class="scroll">{svg_line_chart([r.pass_rate for r in runs], labels, "passrate",
                                      y_label="Pass rate",
                                      target=gate_by_key.get("pass_rate", {}).get("target"),
                                      target_label="target 98%")}</div>
  <p class="formula">Pass rate = passed ÷ (total − skipped). Average = sum of per-run rates ÷ number of runs
     = {e(fmt_pct(metrics["window"]["pass_rate"]))} over {len(runs)} runs.</p>
</section>

<section class="card">
  <h2>Speed</h2>
  <p class="sub">Average duration per executed test, and the tests that cost the most.</p>
  <div class="scroll">{svg_line_chart([r.avg_duration for r in runs], labels, "duration",
                                      y_label="Average duration", unit="s",
                                      target=gate_by_key.get("avg_duration_sec", {}).get("target"),
                                      target_label="target")}</div>
  <p class="formula">Average duration = summed test durations ÷ executed tests.
     Latest: {e(fmt_secs(metrics["avg_duration_sec"]))} ·
     API {e(fmt_secs(metrics["avg_api_duration_sec"]))} ·
     UI {e(fmt_secs(metrics["avg_ui_duration_sec"]))} ·
     suite wall time {e(fmt_secs(metrics["suite_duration_sec"]))}</p>
  <h3 style="margin-top:22px">Slowest tests ({len(runs)}-run average)</h3>
  <div class="scroll">{svg_hbars(slowest)}</div>
</section>

{coverage_section.replace("</section>", cov_trend + "</section>") if cov_trend else coverage_section}

{f'<div class="grid-kpi">{tiles_ui}</div>' if tiles_ui else ""}
{render_ui_coverage_section(ui)}

<section class="card">
  <h2>Suites</h2>
  <p class="sub">Latest run, worst pass rate first.</p>
  {render_suites_table(suites)}
</section>

<section class="card">
  <h2>Needs attention</h2>
  <p class="sub">Repeat failures, tests whose status flips across runs, and tests skipped in every run.
     In-run retries are not configured, so flakiness is detected across the {len(runs)}-run window.</p>
  {render_attention_table(tests, runs)}
</section>

<section class="card">
  <h2>Run history</h2>
  <p class="sub">Every number above is recomputable from this table.</p>
  {render_runs_table(runs, pages_base)}
</section>

<footer>
  <strong>Sources.</strong> Allure <code>widgets/summary.json</code> and
  <code>data/test-cases/*.json</code> per published run; swagger-coverage
  <code>swagger-coverage-results.json</code>. Read-only aggregation — nothing here instruments the tests.<br>
  <strong>Definitions.</strong> Pass rate excludes skipped tests; test coverage is executed ÷ total;
  critical failures are failed + broken (narrowing to blocker/critical if <code>@Severity</code> annotations are added);
  API coverage counts operations called at least once; conditions coverage counts satisfied
  status/parameter/body conditions.<br>
  Generated by <code>scripts/build_quality_dashboard.py</code>.
</footer>
</div>
<div id="tip" role="status" aria-live="polite"></div>"""

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Quality Report · {e(owner_repo or "TeamCity tests")}</title>
<style>{CSS}</style>
</head>
<body>
{body}
<script>{JS}</script>
</body>
</html>"""


def render_uncovered(coverage):
    if not coverage["uncovered"]:
        return ""
    items = "".join(f"<li><code>{e(op)}</code></li>" for op in coverage["uncovered"])
    return (f'<details class="raw"><summary>Never-called operations '
            f'({len(coverage["uncovered"])})</summary>'
            f'<ul class="muted" style="font-size:12.5px;columns:2;column-gap:28px">{items}</ul></details>')


# --------------------------------------------------------------------------------------
# Rolling history — survives Allure report pruning (only ~20 reports are retained)
# --------------------------------------------------------------------------------------
def update_history(dest_dir, runs, coverage):
    path = os.path.join(dest_dir, "history.json")
    history = read_json(path, [])
    if not isinstance(history, list):
        history = []

    by_run = {int(entry["run"]): entry for entry in history
              if isinstance(entry, dict) and str(entry.get("run", "")).isdigit()}

    for run in runs:
        entry = by_run.get(run.number, {})
        entry.update({
            "run": run.number,
            "total": run.total,
            "passed": run.passed,
            "failed": run.failed,
            "broken": run.broken,
            "skipped": run.skipped,
            "pass_rate": run.pass_rate,
            "test_coverage": run.test_coverage,
            "avg_duration_sec": run.avg_duration,
            "suite_duration_sec": run.wall_seconds,
            "critical_failures": run.critical_failures,
            "started_at": run.started_at.isoformat() if run.started_at else None,
        })
        cov = run.coverage or (coverage if run is runs[-1] else None)
        if cov:
            entry.update({
                "api_coverage": cov["api_coverage"],
                "conditions_coverage": cov["conditions_coverage"],
                "full_coverage": cov["full"],
                "partial_coverage": cov["partial"],
                "empty_coverage": cov["empty"],
            })
        by_run[run.number] = entry

    history = [by_run[k] for k in sorted(by_run)][-200:]
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(history, fh, indent=1)
    return history


def write_github_output(path, metrics, coverage, url, runs, latest_url=""):
    """Append step outputs for the workflow to consume (job outputs -> Telegram).

    Values are pre-formatted here so the workflow never does arithmetic in bash.
    `telegram_block` uses the heredoc form GitHub supports for multiline outputs.
    """
    latest = runs[-1]
    gates = metrics["gates"]
    bad = [g["name"] for g in gates if g["state"] == "bad"]
    warn = [g["name"] for g in gates if g["state"] == "warn"]
    if bad:
        verdict, icon = f"{len(bad)} failing", "🔴"
    elif warn:
        verdict, icon = f"{len(warn)} warning", "🟡"
    else:
        verdict, icon = "all passing", "🟢"

    cov_fpe = (f'{metrics["full_coverage"]}/{metrics["partial_coverage"]}/{metrics["empty_coverage"]}'
               if coverage else "n/a")

    scalars = {
        "pass_rate": fmt_pct(metrics["pass_rate"]),
        "total_tests": fmt_int(metrics["total_tests"]),
        "avg_duration": fmt_secs(metrics["avg_duration_sec"]),
        "critical_failures": fmt_int(metrics["critical_failures"]),
        "skipped_tests": fmt_int(metrics["skipped_tests"]),
        "skip_rate": fmt_pct(metrics["skip_rate"]),
        "test_coverage": fmt_pct(metrics["test_coverage"]),
        "api_coverage": fmt_pct(metrics["api_coverage"]),
        "conditions_coverage": fmt_pct(metrics["conditions_coverage"]),
        "full_coverage": fmt_int(metrics["full_coverage"]),
        "partial_coverage": fmt_int(metrics["partial_coverage"]),
        "empty_coverage": fmt_int(metrics["empty_coverage"]),
        "coverage_fpe": cov_fpe,
        "gate_icon": icon,
        "gate_verdict": verdict,
        "gates_failing": ", ".join(bad) if bad else "",
        "gates_warning": ", ".join(warn) if warn else "",
        "run_number": str(latest.number),
        "dashboard_url": url or "",
        "dashboard_latest_url": latest_url or "",
    }

    # Telegram HTML. Kept to a handful of lines so the notification stays scannable.
    block = "\n".join([
        f'<b>📈 Quality Report</b> — gates {icon} {e(verdict)}',
        f'Pass rate: <b>{e(scalars["pass_rate"])}</b> · '
        f'Tests: {e(scalars["total_tests"])} ({e(scalars["skipped_tests"])} skipped) · '
        f'Critical failures: <b>{e(scalars["critical_failures"])}</b>',
        f'Avg duration: {e(scalars["avg_duration"])} · '
        f'Test coverage: {e(scalars["test_coverage"])}',
        f'API coverage: {e(scalars["api_coverage"])} · '
        f'Conditions: {e(scalars["conditions_coverage"])} · '
        f'Full/Partial/Empty: {e(cov_fpe)}',
    ])
    if bad:
        block += f'\n⚠️ Breached: {e(", ".join(bad))}'

    with open(path, "a", encoding="utf-8") as fh:
        for key, value in scalars.items():
            fh.write(f"{key}={value}\n")
        fh.write(f"telegram_block<<QR_EOF_MARKER\n{block}\nQR_EOF_MARKER\n")


def coverage_series(history, runs):
    """Coverage trend from rolling history, aligned to the displayed run window."""
    by_run = {int(h["run"]): h for h in history if str(h.get("run", "")).isdigit()}
    labels = [str(r.number) for r in runs]
    return {
        "labels": labels,
        "api_coverage": [by_run.get(r.number, {}).get("api_coverage") for r in runs],
        "conditions_coverage": [by_run.get(r.number, {}).get("conditions_coverage") for r in runs],
    }


# --------------------------------------------------------------------------------------
# Markdown digest (for $GITHUB_STEP_SUMMARY)
# --------------------------------------------------------------------------------------
def render_markdown(metrics, coverage, runs, url, latest_url=""):
    latest = runs[-1]
    # `url` is the immutable per-run permalink; `latest_url` is the rolling branch path.
    # Both are shown so a stale rolling copy is obvious rather than silently misleading.
    links = []
    if url:
        links.append(f"[Open the full dashboard]({url})")
    if latest_url and latest_url != url:
        links.append(f"[latest for this branch]({latest_url})")
    lines = [
        f"## Quality Report — run #{latest.number}",
        "",
        " · ".join(links),
        "",
        "| Metric | Value |",
        "| --- | ---: |",
        f"| Pass rate | {fmt_pct(metrics['pass_rate'])} |",
        f"| Total tests | {fmt_int(metrics['total_tests'])} |",
        f"| Average duration | {fmt_secs(metrics['avg_duration_sec'])} |",
        f"| Critical failures | {fmt_int(metrics['critical_failures'])} |",
        f"| Skipped tests | {fmt_int(metrics['skipped_tests'])} ({fmt_pct(metrics['skip_rate'])}) |",
        f"| Test coverage | {fmt_pct(metrics['test_coverage'])} |",
        f"| API coverage | {fmt_pct(metrics['api_coverage'])} |",
        f"| Conditions coverage | {fmt_pct(metrics['conditions_coverage'])} |",
        f"| Full coverage | {fmt_int(metrics['full_coverage'])} |",
        f"| Partial coverage | {fmt_int(metrics['partial_coverage'])} |",
        f"| Empty coverage | {fmt_int(metrics['empty_coverage'])} |",
        "",
    ]
    breaches = [g for g in metrics["gates"] if g["state"] == "bad"]
    warns = [g for g in metrics["gates"] if g["state"] == "warn"]
    if breaches:
        lines.append("### Gates failing")
        for g in breaches:
            direction = "≥" if g["direction"] == "minimum" else "≤"
            lines.append(f"- **{g['name']}** — {format_gate_value(g['actual'], g['unit'])} "
                         f"(target {direction} {format_gate_value(g['target'], g['unit'])}). {g['recommendation']}")
        lines.append("")
    if warns:
        lines.append(f"_Warning: {', '.join(g['name'] for g in warns)}_")
        lines.append("")
    lines.append(f"Window: {len(runs)} runs (#{runs[0].number}–#{latest.number}).")
    return "\n".join(line for line in lines if line is not None)


# --------------------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------------------
def main(argv=None):
    parser = argparse.ArgumentParser(description="Build the Quality Report dashboard.")
    parser.add_argument("--site-dir", required=True,
                        help="Directory holding the numbered Allure report dirs (e.g. allure-history)")
    parser.add_argument("--gates", default="quality-gates.json", help="Quality gate targets")
    parser.add_argument("--destination", default="quality",
                        help="Subdirectory of --site-dir to write the dashboard into")
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", ""),
                        help="owner/name, used for links")
    parser.add_argument("--run-number", type=int, default=None,
                        help="Current run number. The build fails if this run has no usable "
                             "Allure report, and the run-scoped permalink is written under it.")
    parser.add_argument("--swagger-results", default=None,
                        help="swagger-coverage-results.json for the current run, if not yet published")
    parser.add_argument("--ui-coverage", default=None,
                        help="ui-route-coverage.json from scripts/build_route_coverage.py")
    parser.add_argument("--window", type=int, default=25, help="Max runs to display")
    parser.add_argument("--markdown-out", default=None, help="Write the Markdown digest here")
    parser.add_argument("--pages-url", default=None,
                        help="Public base URL of the site, used in links and the digest")
    parser.add_argument("--github-output", default=None,
                        help="Append formatted key=value step outputs here (pass $GITHUB_OUTPUT)")
    args = parser.parse_args(argv)

    gates = validate_gates(read_json(args.gates, {}) or {})
    if not gates:
        raise SystemExit(f"No gates loaded from {args.gates}")

    runs = discover_runs(args.site_dir, args.window)
    if not runs:
        raise SystemExit(f"No Allure reports found under {args.site_dir!r} — nothing to build.")

    # The dashboard must describe *this* run. When a build fails before Maven writes
    # allure-results, the run directory is absent or carries no widgets/summary.json,
    # load_run() drops it, and runs[-1] silently becomes the PREVIOUS run — publishing
    # last run's numbers under this run's notification. Fail loudly: no dashboard is
    # better than a stale one that looks current.
    if args.run_number is not None and runs[-1].number != args.run_number:
        seen = ", ".join(f"#{r.number}" for r in runs[-5:]) or "none"
        raise SystemExit(
            f"Run #{args.run_number} has no usable Allure report under {args.site_dir!r} "
            f"(newest usable: #{runs[-1].number}; last discovered: {seen}). "
            f"Refusing to publish a dashboard that would describe an older run."
        )

    latest = runs[-1]
    coverage = latest.coverage
    if coverage is None and args.swagger_results:
        coverage = load_coverage(args.swagger_results)
        latest.coverage = coverage

    ui = load_ui_coverage(args.ui_coverage) if args.ui_coverage else None
    if args.ui_coverage and ui is None:
        print(f"warning: {args.ui_coverage} missing or unreadable — UI coverage card will "
              f"render as unavailable", file=sys.stderr)

    metrics = build_metrics(runs, gates, coverage)

    dest_dir = os.path.join(args.site_dir, args.destination)
    os.makedirs(dest_dir, exist_ok=True)

    history = update_history(dest_dir, runs, coverage)
    cov_series = coverage_series(history, runs)

    def render_at(depth):
        """Render the page for a location `depth` directories below the site root.

        With --pages-url the base is absolute and depth is irrelevant; without it the
        page has to reach the Allure reports relatively, and the two copies we write
        sit at different depths.
        """
        base = (args.pages_url.rstrip("/") if args.pages_url
                else ("/".join([".."] * depth) if depth else "."))
        return base, render_page(runs, metrics, coverage, cov_series,
                                 {"repo": args.repo, "pages_base": base}, ui=ui)

    dest_depth = len([p for p in args.destination.split("/") if p])
    pages_base, page = render_at(dest_depth)
    with open(os.path.join(dest_dir, "index.html"), "w", encoding="utf-8") as fh:
        fh.write(page)

    # Run-scoped permalink. <destination> is stable per branch, so it always serves
    # "whatever was published last" — a notification linking it can show an older run
    # when this run's Pages deploy lags, or when a sibling branch's deploy prunes the
    # tree. The copy under <run>/ is immutable and lives inside the run directory the
    # Allure action already retains.
    run_url = ""
    if args.run_number is not None:
        run_dir = os.path.join(args.site_dir, str(args.run_number))
        if os.path.isdir(run_dir):
            _, run_page = render_at(1)
            with open(os.path.join(run_dir, "quality.html"), "w", encoding="utf-8") as fh:
                fh.write(run_page)
            if args.pages_url:
                run_url = f"{pages_base}/{args.run_number}/quality.html"
        else:
            print(f"warning: {run_dir} missing — no run-scoped permalink written",
                  file=sys.stderr)

    latest_url = f"{pages_base}/{args.destination}/" if args.pages_url else ""
    # Prefer the immutable permalink for notifications; fall back to the rolling path.
    url = run_url or latest_url

    public = {k: v for k, v in metrics.items() if not k.startswith("_") and k != "gates"}
    public["gates"] = [{k: g[k] for k in ("key", "name", "actual", "target", "state")}
                       for g in metrics["gates"]]
    public["generated_at"] = datetime.now(timezone.utc).isoformat()
    public["dashboard_url"] = url or None
    public["dashboard_latest_url"] = latest_url or None
    if ui:
        public["ui_page_coverage"] = ui["page_coverage"]
        public["ui_route_coverage"] = ui["route_coverage"]
        public["ui_pages_total"] = ui["pages_total"]
        public["ui_pages_untouched"] = ui["pages_untouched_count"]
    with open(os.path.join(dest_dir, "summary.json"), "w", encoding="utf-8") as fh:
        json.dump(public, fh, indent=1, default=str)

    markdown = render_markdown(metrics, coverage, runs, url, latest_url)
    if args.markdown_out:
        with open(args.markdown_out, "w", encoding="utf-8") as fh:
            fh.write(markdown + "\n")

    if args.github_output:
        write_github_output(args.github_output, metrics, coverage, url, runs, latest_url)

    breaches = [g["name"] for g in metrics["gates"] if g["state"] == "bad"]
    print(f"Quality Report written to {dest_dir}/index.html", file=sys.stderr)
    print(f"  window          : {len(runs)} runs (#{runs[0].number}-#{latest.number})", file=sys.stderr)
    print(f"  pass rate       : {fmt_pct(metrics['pass_rate'])}", file=sys.stderr)
    print(f"  total tests     : {metrics['total_tests']} "
          f"({metrics['executed_tests']} executed, {metrics['skipped_tests']} skipped)", file=sys.stderr)
    print(f"  avg duration    : {fmt_secs(metrics['avg_duration_sec'])}", file=sys.stderr)
    print(f"  critical fails  : {metrics['critical_failures']}", file=sys.stderr)
    print(f"  API coverage    : {fmt_pct(metrics['api_coverage'])}", file=sys.stderr)
    print(f"  conditions      : {fmt_pct(metrics['conditions_coverage'])}", file=sys.stderr)
    print(f"  gates failing   : {', '.join(breaches) if breaches else 'none'}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
