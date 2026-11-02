import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Proxy REST (transcoded) calls to Envoy to avoid CORS in dev
      proxy: { "/v1": env.VITE_API_BASE_URL || "http://localhost:8080" },
    },
    test: {
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      coverage: {
        provider: "v8",
        include: ["src/**/*.{ts,tsx}"],
        exclude: ["src/main.tsx", "src/test/**", "src/**/*.test.{ts,tsx}", "src/api/types.ts"],
        thresholds: { lines: 100, branches: 100, functions: 100, statements: 100 },
      },
    },
  };
});
