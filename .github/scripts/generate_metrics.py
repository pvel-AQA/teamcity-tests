import os
import json
import glob

def parse_allure_results(results_dir):
    json_files = glob.glob(os.path.join(results_dir, "*-result.json"))

    total_tests = 0
    passed = 0
    failed = 0
    broken = 0
    flaky = 0

    ui_durations = []
    api_durations = []
    browser_stats = {"chrome": {"total": 0, "passed": 0, "failed": 0, "durations": []},
                     "firefox": {"total": 0, "passed": 0, "failed": 0, "durations": []}}
    slowest_tests = []

    for file_path in json_files:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except Exception:
            continue

        status = data.get("status", "unknown")
        duration_sec = (data.get("stop", 0) - data.get("start", 0)) / 1000.0
        test_name = data.get("fullName", data.get("name", "Unknown Test"))
        is_flaky = data.get("flaky", False) or data.get("statusDetails", {}).get("flaky", False)

        # Анализ параметров/окружения
        parameters = {p.get("name"): p.get("value") for p in data.get("parameters", [])}
        browser = parameters.get("browser", "chrome").lower()
        if browser not in browser_stats:
            browser_stats[browser] = {"total": 0, "passed": 0, "failed": 0, "durations": []}

        total_tests += 1
        browser_stats[browser]["total"] += 1

        if status == "passed":
            passed += 1
            browser_stats[browser]["passed"] += 1
        elif status == "failed":
            failed += 1
            browser_stats[browser]["failed"] += 1
        elif status == "broken":
            broken += 1

        if is_flaky:
            flaky += 1

        # Классификация UI vs API
        if "ui" in test_name.lower() or "browser" in parameters:
            ui_durations.append(duration_sec)
        else:
            api_durations.append(duration_sec)

        browser_stats[browser]["durations"].append(duration_sec)
        slowest_tests.append({"name": test_name, "duration": round(duration_sec, 2), "status": status, "browser": browser})

    slowest_tests = sorted(slowest_tests, key=lambda x: x["duration"], reverse=True)[:5]

    pass_rate = round((passed / total_tests * 100), 2) if total_tests > 0 else 0
    fail_rate = round((failed / total_tests * 100), 2) if total_tests > 0 else 0
    broken_rate = round((broken / total_tests * 100), 2) if total_tests > 0 else 0
    flaky_rate = round((flaky / total_tests * 100), 2) if total_tests > 0 else 0

    avg_ui = round(sum(ui_durations) / len(ui_durations), 2) if ui_durations else 0
    avg_api = round(sum(api_durations) / len(api_durations), 2) if api_durations else 0

    return {
        "total": total_tests,
        "pass_rate": pass_rate,
        "fail_rate": fail_rate,
        "broken_rate": broken_rate,
        "flaky_rate": flaky_rate,
        "avg_ui_duration": avg_ui,
        "avg_api_duration": avg_api,
        "browser_stats": browser_stats,
        "slowest_tests": slowest_tests
    }

def generate_html_report(metrics, output_dir):
    os.makedirs(output_dir, exist_ok=True)

    # Сравнение с Quality Gates
    gates = [
        {"metric": "Average Pass Rate", "val": f"{metrics['pass_rate']}%", "target": ">= 98.0%", "ok": metrics['pass_rate'] >= 98.0},
        {"metric": "Average Fail Rate", "val": f"{metrics['fail_rate']}%", "target": "<= 2.0%", "ok": metrics['fail_rate'] <= 2.0},
        {"metric": "Average Broken Rate", "val": f"{metrics['broken_rate']}%", "target": "<= 1.0%", "ok": metrics['broken_rate'] <= 1.0},
        {"metric": "Flaky Rate", "val": f"{metrics['flaky_rate']}%", "target": "<= 1.0%", "ok": metrics['flaky_rate'] <= 1.0},
        {"metric": "Avg UI Test Duration", "val": f"{metrics['avg_ui_duration']}s", "target": "<= 12.0s", "ok": metrics['avg_ui_duration'] <= 12.0},
    ]

    gates_rows = "".join([
        f"<tr><td>{g['metric']}</td><td>{g['val']}</td><td>{g['target']}</td><td class='{'status-ok' if g['ok'] else 'status-fail'}'>{'OK' if g['ok'] else 'FAIL'}</td></tr>"
        for g in gates
    ])

    slow_rows = "".join([
        f"<tr><td>{t['browser'].capitalize()}</td><td><code>{t['name']}</code></td><td>{t['duration']}s</td><td class='status-{t['status']}'>{t['status'].upper()}</td></tr>"
        for t in metrics["slowest_tests"]
    ])

    html_content = f"""<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>TeamCity QA Metrics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        h1 {{ font-size: 24px; font-weight: 700; border-bottom: 2px solid #334155; padding-bottom: 12px; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 24px 0; }}
        .card {{ background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 16px; text-align: center; }}
        .card .val {{ font-size: 28px; font-weight: bold; margin-top: 8px; }}
        .card .lbl {{ font-size: 12px; color: #94a3b8; text-transform: uppercase; }}
        .pass {{ color: #22c55e; }} .fail {{ color: #ef4444; }} .warn {{ color: #eab308; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 16px; background: #1e293b; border-radius: 8px; overflow: hidden; }}
        th, td {{ padding: 12px 16px; text-align: left; border-bottom: 1px solid #334155; font-size: 14px; }}
        th {{ background: #1e293b; color: #94a3b8; font-weight: 600; }}
        .status-ok {{ color: #22c55e; font-weight: bold; }}
        .status-fail {{ color: #ef4444; font-weight: bold; }}
        .status-passed {{ color: #22c55e; }} .status-failed {{ color: #ef4444; }}
        .chart-container {{ background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 20px; margin-top: 24px; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>TeamCity QA Metrics Dashboard</h1>
        <p style="color: #94a3b8;">Качество автотестов и метрики прогона (GitHub Actions Run)</p>

        <div class="grid">
            <div class="card"><div class="lbl">Pass Rate</div><div class="val pass">{metrics['pass_rate']}%</div></div>
            <div class="card"><div class="lbl">Fail Rate</div><div class="val fail">{metrics['fail_rate']}%</div></div>
            <div class="card"><div class="lbl">Flaky Rate</div><div class="val warn">{metrics['flaky_rate']}%</div></div>
            <div class="card"><div class="lbl">Avg UI Duration</div><div class="val">{metrics['avg_ui_duration']}s</div></div>
            <div class="card"><div class="lbl">Avg API Duration</div><div class="val">{metrics['avg_api_duration']}s</div></div>
        </div>

        <h2>🚦 Quality Gates</h2>
        <table>
            <thead><tr><th>Metric</th><th>Current Value</th><th>Target</th><th>Status</th></tr></thead>
            <tbody>{gates_rows}</tbody>
        </table>

        <div class="chart-container">
            <h2>📊 Test Outcome Distribution</h2>
            <div style="max-width: 300px; margin: 0 auto;">
                <canvas id="statusChart"></canvas>
            </div>
        </div>

        <h2>🐌 Top-5 Slowest Tests</h2>
        <table>
            <thead><tr><th>Browser</th><th>Test Name</th><th>Duration</th><th>Status</th></tr></thead>
            <tbody>{slow_rows}</tbody>
        </table>
    </div>

    <script>
        const ctx = document.getElementById('statusChart').getContext('2d');
        new Chart(ctx, {{
            type: 'doughnut',
            data: {{
                labels: ['Passed', 'Failed', 'Broken'],
                datasets: [{{
                    data: [{metrics['pass_rate']}, {metrics['fail_rate']}, {metrics['broken_rate']}],
                    backgroundColor: ['#22c55e', '#ef4444', '#eab308']
                }}]
            }},
            options: {{ responsive: true, plugins: {{ legend: {{ labels: {{ color: '#f8fafc' }} }} }} }}
        }});
    </script>
</body>
</html>"""

    with open(os.path.join(output_dir, "index.html"), "w", encoding="utf-8") as f:
        f.write(html_content)

if __name__ == "__main__":
    metrics = parse_allure_results("target/allure-results")
    generate_html_report(metrics, "target/quality-dashboard")
    print("Дашборд успешно сгенерирован в target/quality-dashboard/index.html")
