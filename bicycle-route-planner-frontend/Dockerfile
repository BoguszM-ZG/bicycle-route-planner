# syntax=docker/dockerfile:1

# -------------------------------------------------------------
# Stage 1: Build Angular application with Node.js
# -------------------------------------------------------------
FROM node:24-alpine AS builder

WORKDIR /app

# Copy dependency definitions for layer caching
COPY package.json package-lock.json ./
RUN npm ci

# Copy project source files
COPY . .

# Build production bundle
RUN npm run build

# -------------------------------------------------------------
# Stage 2: Serve production assets via Nginx
# -------------------------------------------------------------
FROM nginx:1.27-alpine AS runner

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy compiled Angular SPA from builder stage
COPY --from=builder /app/dist/bicycle-route-planner-frontend/browser /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Configure directory permissions and run as non-root user 'nginx'
RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /var/log/nginx /etc/nginx/conf.d /usr/share/nginx/html

USER nginx

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
