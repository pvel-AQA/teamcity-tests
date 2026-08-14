import { defineConfig } from "allure";

export default defineConfig({
    name: "Cross-Browser Test Report",
    // Указываем Allure 3, где хранить сквозную историю для трендов
    historyPath: "allure-history/history.jsonl",
    output: "./allure-report",
    environments: {
        chrome: {
            name: "Google Chrome",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("chrome")),
        },
        firefox: {
            name: "Mozilla Firefox",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("firefox")),
        },
        safari: {
            name: "Apple Safari",
            matcher: ({ labels }) =>
                labels.some(l => l.name === "browser" && l.value.toLowerCase().includes("safari")),
        },
    },
});
