FROM node:20-alpine
WORKDIR /app
COPY chat-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY chat-service/ ./
COPY shared /shared
EXPOSE 3005
CMD ["node", "src/app.js"]
