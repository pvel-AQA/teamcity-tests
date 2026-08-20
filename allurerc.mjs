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
            // Только базовые правила без фильтров
            {
                id: "basic-quality",
                description: "Basic quality checks",
                expect: {
                    successRate: 0.95,
                    maxFailures: 5,
                    minTestsCount: 10,
                    maxDuration: 60000,
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
