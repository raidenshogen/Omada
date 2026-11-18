# Build the React application into the image served by Nginx.
FROM node:20-alpine AS frontend-build

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Nginx reverse proxy for Omada — SSL termination + backend proxy
FROM nginx:alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/certs/ /etc/nginx/certs/
COPY --from=frontend-build /app/dist/ /usr/share/nginx/html/

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
