class CustomerService {
  constructor(userRepository) {
    this.userRepository = userRepository;
  }

  getMe(userId, actorType) {
    return this.ensureProfile(userId, actorType);
  }

  updateMe(userId, actorType, payload) {
    const existing = this.ensureProfile(userId, actorType);
    const updated = {
      ...existing,
      ...payload,
      userId,
      actorType
    };
    return this.userRepository.save(updated);
  }

  requestMobileVerify(userId, actorType) {
    this.ensureProfile(userId, actorType);
    return { challengeId: `MV-${Date.now()}`, userId, channel: 'sms' };
  }

  confirmMobileVerify(userId, actorType) {
    const existing = this.ensureProfile(userId, actorType);
    existing.mobileVerified = true;
    this.userRepository.save(existing);
    return { verified: true, userId };
  }

  sendNotification(type, payload, actorType) {
    return {
      accepted: true,
      type,
      actorType,
      notificationId: `NTF-${Date.now()}`,
      payload
    };
  }

  health(mode) {
    return {
      status: 'UP',
      details: {
        mode,
        service: 'customer-service',
        storage: 'in-memory'
      }
    };
  }

  ensureProfile(userId, actorType) {
    const existing = this.userRepository.findById(userId);
    if (existing) {
      return existing;
    }

    return this.userRepository.save({
      userId,
      actorType,
      name: actorType === 'corp' ? 'Corp Staff' : 'Customer User',
      email: `${userId.toLowerCase()}@skyfly.dev`,
      mobile: '+910000000000',
      mobileVerified: false,
      preferences: {}
    });
  }
}

module.exports = { CustomerService };
