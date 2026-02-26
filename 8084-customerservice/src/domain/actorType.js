const ActorType = Object.freeze({
  CUSTOMER: 'customer',
  CORP: 'corp'
});

function actorTypeFromHeader(value) {
  if (!value) {
    return ActorType.CUSTOMER;
  }
  return String(value).toLowerCase() === ActorType.CORP ? ActorType.CORP : ActorType.CUSTOMER;
}

module.exports = { ActorType, actorTypeFromHeader };
