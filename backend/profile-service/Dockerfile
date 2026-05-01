FROM node:20-alpine
RUN apk add --no-cache python3 make g++
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY . .
EXPOSE 3002
CMD ["node", "src/app.js"]
