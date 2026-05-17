FROM node:20-alpine
WORKDIR /app
COPY admin-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY admin-service/ ./
COPY shared /shared
EXPOSE 3011
CMD ["node", "src/app.js"]
