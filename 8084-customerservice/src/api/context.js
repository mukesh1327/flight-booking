const { actorTypeFromHeader } = require('../domain/actorType');

function resolveContext(req) {
  const userId = req.header('X-User-Id') || 'U-CUSTOMER-1';
  const actorType = actorTypeFromHeader(req.header('X-Actor-Type'));
  return { userId, actorType };
}

module.exports = { resolveContext };
