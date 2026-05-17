FROM node:20-alpine
WORKDIR /app
COPY notification-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY notification-service/ ./
COPY shared /shared
EXPOSE 3006
CMD ["node", "src/app.js"]
