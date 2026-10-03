const { PrismaClient } = require('@prisma/client');
const { passwordHash } = require('../dist/core');

const url = new URL(process.env.DATABASE_URL);
if (process.env.NODE_ENV === 'production' || !['localhost', '127.0.0.1'].includes(url.hostname)) {
  throw new Error('Test account seed is local development only');
}

const db = new PrismaClient();
db.user.upsert({
  where: { email: 'test@gmail.com' },
  create: { email: 'test@gmail.com', name: 'Test User', passwordHash: passwordHash('test@123') },
  update: { passwordHash: passwordHash('test@123') },
}).then(async user => {
  const month = new Date().toISOString().slice(0, 7);
  await db.budget.upsert({
    where: { ownerId_month_currencyCode: { ownerId: user.id, month, currencyCode: 'INR' } },
    create: { ownerId: user.id, month, currencyCode: 'INR', amountMinor: 1_000_000n },
    update: {},
  });
  console.log('Local test account ready: test@gmail.com');
})
  .finally(() => db.$disconnect());
