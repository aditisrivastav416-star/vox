import { Injectable, CanActivate, ExecutionContext, UnauthorizedException, BadRequestException, OnModuleDestroy } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';
import { createHash, randomBytes, scryptSync, timingSafeEqual, randomUUID } from 'node:crypto';

@Injectable()
export class Db extends PrismaClient implements OnModuleDestroy {
  async onModuleDestroy() { await this.$disconnect(); }
}

export const uuid = () => randomUUID();
export const hash = (s: string) => createHash('sha256').update(s).digest('hex');
export function passwordHash(password: string): string {
  const salt = randomBytes(16).toString('hex');
  return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`;
}
export function passwordMatches(password: string, stored: string): boolean {
  const [salt, value] = stored.split(':');
  if (!salt || !value) return false;
  const a = scryptSync(password, salt, 64), b = Buffer.from(value, 'hex');
  return a.length === b.length && timingSafeEqual(a, b);
}
export function minor(value: unknown): bigint {
  if (typeof value !== 'number' && typeof value !== 'string') throw new BadRequestException('amountMinor must be an integer');
  if (!/^(0|[1-9]\d*)$/.test(String(value))) throw new BadRequestException('amountMinor must be nonnegative integer');
  return BigInt(value);
}
export function money(amountMinor: bigint, currencyCode: string) { return { amountMinor: amountMinor.toString(), currencyCode }; }
export function date(value: string): Date {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) throw new BadRequestException('Invalid date');
  return d;
}
export function publicUser(user: { id: string; email: string | null; name: string; currencyCode: string; phone: string | null; timeZone: string; locale: string }) {
  return { id: user.id, email: user.email, name: user.name, currencyCode: user.currencyCode, phone: user.phone, timeZone: user.timeZone, locale: user.locale };
}

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private db: Db) {}
  async canActivate(ctx: ExecutionContext) {
    const req = ctx.switchToHttp().getRequest();
    const token = /^Bearer (\S+)$/.exec(req.headers.authorization || '')?.[1];
    if (!token) throw new UnauthorizedException();
    const session = await this.db.session.findUnique({ where: { tokenHash: hash(token) }, include: { user: true } });
    if (!session || session.expiresAt < new Date()) throw new UnauthorizedException();
    req.user = session.user;
    req.session = session;
    return true;
  }
}
