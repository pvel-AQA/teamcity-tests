import { defineConfig } from "allure";

export default defineConfig({
    name: "Cross-Browser Test Report",
    // Указываем Allure 3, где хранить сквозную историю для трендов
    historyPath: process.env.ALLURE_HISTORY_PATH || "./allure-history/history.jsonl",
    //appendHistory: true,
    output: "./allure-report", // <-- Это дефолт для локального запуска. В CI он перекроется флагом --output

    // Quality Gates - автоматическая проверка качества тестового прогона
    qualityGate: {
        rules: [
            {
                id: "api-tests-quality",
                description: "API tests must have 100% success rate",
                use: "allure",
                // Правильный формат - функция фильтрации
                filter: (test) => test.package && test.package.includes("api"),
                expect: {
                    successRate: 1.0,
                    maxFailures: 0,
                    minTestsCount: 10,
                }
            },
            {
                id: "ui-tests-quality",
                description: "UI tests must have at least 95% success rate",
                use: "allure",
                filter: (test) => {
                    const env = test.environment || test.env;
                    return env && (env === "chrome" || env === "firefox");
                },
                expect: {
                    successRate: 0.95,
                    maxFailures: 2,
                    minTestsCount: 20,
                    maxDuration: 30000,
                }
            },
            {
                id: "critical-path",
                description: "Critical path tests must all pass",
                use: "allure",
                filter: (test) => {
                    return test.labels && test.labels.some(label =>
                        label.name === "severity" && label.value === "critical"
                    );
                },
                expect: {
                    successRate: 1.0,
                    maxFailures: 0,
                    maxDuration: 60000,
                }
            },
            {
                id: "browser-coverage",
                description: "Tests must run on all browsers",
                use: "allure",
                // Без фильтрации - проверяем все тесты
                expect: {
                    successRate: 0.90,
                    minTestsCount: 50,
                    maxFailures: 5,
                    maxDuration: 60000,
                }
            },
            {
                id: "no-flaky-tests",
                description: "No flaky tests allowed",
                use: "allure",
                expect: {
                    successRate: 0.98,
                    maxRetries: 10,
                }
            },
            {
                id: "performance-check",
                description: "Tests should not be too slow",
                use: "allure",
                expect: {
                    maxDuration: 60000,
                    maxTotalDuration: 600000,
                }
            }
        ]
    },

    environments: {
        api: {
            name: "API Tests",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "package" && l.value.toLowerCase().includes("api")) ||
                labels.some(l => l.name === "testClass" && l.value.toLowerCase().includes("api"))
        },
        chrome: {
            name: "Google Chrome",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("chrome")) &&
                !labels.some(l => l.name === "package" && l.value.toLowerCase().includes("api"))
        },
        firefox: {
            name: "Mozilla Firefox",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("firefox")) &&
                !labels.some(l => l.name === "package" && l.value.toLowerCase().includes("api"))
        },
        safari: {
            name: "Apple Safari",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("safari")) &&
                !labels.some(l => l.name === "package" && l.value.toLowerCase().includes("api"))
        },
    },
});
