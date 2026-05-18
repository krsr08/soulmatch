FROM node:20-alpine
WORKDIR /app
COPY search-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY search-service/ ./
COPY shared /shared
EXPOSE 3004
CMD ["node", "src/app.js"]
