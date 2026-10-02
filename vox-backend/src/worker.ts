import { Db } from './core';
import { generateDue } from './recurring';

const db = new Db();
async function tick() {
  try { await generateDue(db); } catch (error) { console.error('Recurring generation failed', error); }
}
void tick();
setInterval(tick, 60_000);
