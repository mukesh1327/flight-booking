FROM node:22-alpine

WORKDIR /app
COPY package.json .
RUN npm install
COPY index.js .
COPY src ./src

EXPOSE 8084
CMD ["npm", "run", "start"]
