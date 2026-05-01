FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --audit=false --fund=false
COPY . .
EXPOSE 3000
ENV REACT_APP_ADMIN_API_URL=http://localhost:3011/api/v1/admin
CMD ["npm", "start"]
