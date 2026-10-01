const { test } = require('node:test');
const assert = require('node:assert/strict');
const { next } = require('../dist/recurring');

test('recurring dates advance and clamp month ends', () => {
  assert.equal(next(new Date('2026-10-01'), 'DAILY').toISOString().slice(0,10), '2026-10-02');
  assert.equal(next(new Date('2026-10-01'), 'WEEKLY').toISOString().slice(0,10), '2026-10-08');
  assert.equal(next(new Date('2026-10-01'), 'CUSTOM', 10).toISOString().slice(0,10), '2026-10-11');
  assert.equal(next(new Date('2026-01-31'), 'MONTHLY').toISOString().slice(0,10), '2026-02-28');
});
