# ==============================================================================
# Edu-AI Trace Web & API Runtime Dockerfile
#
# BACKEND ENTRYPOINT INSTRUCTION:
# The backend administrative interface opens at:
#   -> backend/templates/admin/login.html (served at / and /admin/login)
# The public user onboarding interface opens at:
#   -> backend/templates/public/signup.html (served at /signup, /Edu_AI/signup.html)
# ==============================================================================

# Stage 1: Build the frontend static assets
FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Runtime image
FROM node:20-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

COPY package*.json ./
RUN npm ci --only=production

COPY --from=build /app/dist ./dist

# Expose standard port 3000
EXPOSE 3000

CMD ["node", "dist/server.cjs"]
