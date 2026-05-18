FROM node:20-alpine
WORKDIR /app
COPY auth-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY auth-service/ ./
COPY shared /shared
EXPOSE 3001
CMD ["node", "src/app.js"]
