const { createApp } = require('./src/api/app');

const app = createApp();
const port = process.env.PORT || 8084;

app.listen(port, '0.0.0.0', () => {
  console.log(`customer-service listening on :${port}`);
});
