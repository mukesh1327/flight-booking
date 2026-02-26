class InMemoryUserRepository {
  constructor() {
    this.users = new Map();
    this.users.set('U-CUSTOMER-1', {
      userId: 'U-CUSTOMER-1',
      actorType: 'customer',
      name: 'SkyFly Customer',
      email: 'customer@skyfly.dev',
      mobile: '+911234567890',
      mobileVerified: false,
      preferences: { cabin: 'economy', seat: 'aisle' }
    });
    this.users.set('U-CORP-1', {
      userId: 'U-CORP-1',
      actorType: 'corp',
      name: 'SkyFly Staff',
      email: 'staff@skyfly.dev',
      mobile: '+919876543210',
      mobileVerified: true,
      preferences: { dashboard: 'ops' }
    });
  }

  findById(userId) {
    return this.users.get(userId);
  }

  save(user) {
    this.users.set(user.userId, user);
    return user;
  }
}

module.exports = { InMemoryUserRepository };
