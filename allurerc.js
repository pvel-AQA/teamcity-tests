import { defineConfig } from "allure";

export default defineConfig({
    name: "Cross-Browser Test Report",
    // Указываем Allure 3, где хранить сквозную историю для трендов
    historyPath: process.env.ALLURE_HISTORY_PATH || "./allure-history/history.jsonl",
    appendHistory: true,
    output: "./allure-report", // <-- Это дефолт для локального запуска. В CI он перекроется флагом --output

    // Quality Gates - автоматическая проверка качества тестового прогона
    qualityGate: {
        rules: [
            {
                id: "api-tests-quality",
                description: "API tests must have 100% success rate",
                // Применяем правило только к API тестам
                matcher: ({ environment }) => environment === "api",
                // Правила для этого набора
                successRate: 1.0,        // 100% успешных тестов
                maxFailures: 0,           // Ни одного упавшего теста
                minTestsCount: 10,        // Минимум 10 тестов должно быть выполнено
            },
            {
                id: "ui-tests-quality",
                description: "UI tests must have at least 95% success rate",
                // Применяем к Chrome и Firefox (исключаем API)
                matcher: ({ environment }) =>
                    environment === "chrome" || environment === "firefox",
                successRate: 0.95,        // 95% успешных тестов
                maxFailures: 2,           // Не более 2 упавших тестов
                minTestsCount: 20,        // Минимум 20 тестов
                maxDuration: 30000,       // Максимальная длительность теста 30 секунд
            },
            {
                id: "critical-path",
                description: "Critical path tests must all pass",
                // Применяем к тестам с определенной меткой
                matcher: ({ labels }) =>
                    labels.some(l => l.name === "severity" && l.value.toLowerCase() === "critical"),
                successRate: 1.0,
                maxFailures: 0,
                maxDuration: 60000,       // Критические тесты могут идти дольше (60 сек)
            },
            {
                id: "browser-coverage",
                description: "Tests must run on all browsers",
                // Применяем ко всему прогону в целом
                matcher: ({ results }) => {
                    // Проверяем, что тесты запускались на всех браузерах
                    const browsers = new Set();
                    results.forEach(r => {
                        const browserLabel = r.labels?.find(l => l.name === "browser");
                        if (browserLabel) browsers.add(browserLabel.value);
                    });
                    return browsers.size >= 2; // Как минимум 2 браузера
                },
                successRate: 0.90,         // Минимум 90% успешных тестов
                minTestsCount: 50,         // Минимум 50 тестов всего
                maxFailures: 5,            // Не более 5 упавших
                maxDuration: 60000,        // Максимальная длительность 60 секунд
            },
            {
                id: "no-flaky-tests",
                description: "No flaky tests allowed",
                // Применяем ко всем тестам
                matcher: () => true,
                // Проверяем, что нет флаки-тестов (тестов, которые иногда падают)
                maxRetries: 10,             // Максимум повторных прогонов
                successRate: 0.98,         // 98% успешных
            },
            {
                id: "performance-check",
                description: "Tests should not be too slow",
                matcher: ({ duration }) => duration > 0, // Все тесты
                maxDuration: 60000,        // Максимум 60 секунд на тест
                maxTotalDuration: 600000,  // Максимум 10 минут на весь прогон
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
