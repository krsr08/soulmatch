FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --audit=false --fund=false
COPY . .
EXPOSE 3007
CMD ["node", "src/app.js"]
