import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// GitHub Pages serves the site at /<repo-name>/; the deploy workflow passes that in as VITE_BASE.
export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE || '/',
});
