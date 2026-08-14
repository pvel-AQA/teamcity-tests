import { defineConfig } from "allure";

export default defineConfig({
    name: "Cross-Browser Test Report",
    // Указываем Allure 3, где хранить сквозную историю для трендов
    historyPath: "allure-history/history.jsonl",
    output: "./allure-report",
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
