FROM node:20-alpine
RUN apk add --no-cache python3 make g++
WORKDIR /app
COPY profile-service/package*.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY profile-service/ ./
COPY shared /shared
EXPOSE 3002
CMD ["node", "src/app.js"]
