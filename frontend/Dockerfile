# syntax=docker/dockerfile:1

# ---- Build stage ------------------------------------------------------------
# Builds the production Angular bundle. Quality gates (format, lint, test) already run in CI via
# `npm run format:check && npm run lint && npm test`, so this stage only builds the artifact.
FROM node:22-alpine AS build
WORKDIR /workspace

# Copied and installed before the source tree, so this layer is only invalidated when a dependency
# actually changes rather than on every code change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage ------------------------------------------------------------
# nginx serves the built SPA. Traefik (see docker-compose.prod.yml) terminates HTTPS and routes
# /api/* to the backend under the same origin as this container (see frontend/CLAUDE.md).
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
