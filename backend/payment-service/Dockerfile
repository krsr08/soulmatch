FROM node:20-alpine
WORKDIR /app
COPY payment-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY payment-service/ ./
COPY shared /shared
EXPOSE 3007
CMD ["node", "src/app.js"]
