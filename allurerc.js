import { defineConfig } from "allure";

// Динамически определяем папку вывода:
// Если запущено в CI, пишем в allure-history/<номер_ранга>, иначе в локальный ./allure-report
const outputDir = process.env.ALLURE_OUTPUT_DIR || "./allure-report";

export default defineConfig({
    name: "Cross-Browser Test Report",
    // Указываем Allure 3, где хранить сквозную историю для трендов
    historyPath: "allure-history/history.jsonl",
    output: outputDir, // <-- Теперь Allure сам гарантирует создание этой папки и подпапок плагинов
    plugins: {
        awesome: {
            options: {
                reportName: "Cross-Browser & API Test Report"
            }
        }
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
