const express = require('express');
const { InMemoryUserRepository } = require('../infrastructure/inMemoryUserRepository');
const { CustomerService } = require('../application/customerService');
const { resolveContext } = require('./context');

function createApp() {
  const app = express();
  app.use(express.json());

  const service = new CustomerService(new InMemoryUserRepository());

  app.get('/api/v1/health', (_req, res) => res.json(service.health('health')));
  app.get('/api/v1/health/live', (_req, res) => res.json(service.health('live')));
  app.get('/api/v1/health/ready', (_req, res) => res.json(service.health('ready')));

  app.get('/api/v1/users/me', (req, res) => {
    const { userId, actorType } = resolveContext(req);
    res.json(service.getMe(userId, actorType));
  });

  app.patch('/api/v1/users/me', (req, res) => {
    const { userId, actorType } = resolveContext(req);
    res.json(service.updateMe(userId, actorType, req.body || {}));
  });

  app.post('/api/v1/users/me/mobile/verify/request', (req, res) => {
    const { userId, actorType } = resolveContext(req);
    res.json(service.requestMobileVerify(userId, actorType));
  });

  app.post('/api/v1/users/me/mobile/verify/confirm', (req, res) => {
    const { userId, actorType } = resolveContext(req);
    res.json(service.confirmMobileVerify(userId, actorType));
  });

  app.post('/api/v1/notifications/email', (req, res) => {
    const { actorType } = resolveContext(req);
    res.json(service.sendNotification('email', req.body || {}, actorType));
  });

  app.post('/api/v1/notifications/sms', (req, res) => {
    const { actorType } = resolveContext(req);
    res.json(service.sendNotification('sms', req.body || {}, actorType));
  });

  app.post('/api/v1/notifications/push', (req, res) => {
    const { actorType } = resolveContext(req);
    res.json(service.sendNotification('push', req.body || {}, actorType));
  });

  return app;
}

module.exports = { createApp };
