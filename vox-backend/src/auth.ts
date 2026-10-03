import { Body, Controller, Delete, Get, Post, Put, Req, UseGuards, BadRequestException, ConflictException, UnauthorizedException, ServiceUnavailableException, HttpException, HttpStatus } from '@nestjs/common';
import { IsOptional, IsString, Length, Matches } from 'class-validator';
import { Db, AuthGuard, hash, passwordHash, passwordMatches, publicUser } from './core';
import { randomBytes, randomInt } from 'node:crypto';

class Credentials {
  @IsString() identifier!: string;
  @IsString() @Length(8, 128) password!: string;
  @IsOptional() @IsString() @Length(1, 100) name?: string;
}
class SignupStart { @IsString() identifier!: string; }
class SignupVerify extends SignupStart { @Matches(/^\d{6}$/) code!: string; }
class SignupFinish extends SignupVerify {
  @IsString() @Length(8, 128) password!: string;
  @IsString() confirmPassword!: string;
  @IsOptional() @IsString() @Length(1, 100) name?: string;
}
function contact(value: string) {
  const id = value.trim().toLowerCase();
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(id) || /^\+[1-9]\d{7,14}$/.test(id)) return id;
  throw new BadRequestException('Enter an email or phone in international format');
}
async function deliver(id: string, code: string) {
  if (id.includes('@')) {
    if (!process.env.RESEND_API_KEY || !process.env.OTP_FROM_EMAIL) throw new ServiceUnavailableException('Email delivery is not configured');
    const response = await fetch('https://api.resend.com/emails', { method: 'POST', headers: { Authorization: `Bearer ${process.env.RESEND_API_KEY}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ from: process.env.OTP_FROM_EMAIL, to: id, subject: 'Your Vox code', text: `Your Vox signup code is ${code}. It expires in 10 minutes.` }) });
    if (!response.ok) throw new ServiceUnavailableException('Email delivery failed');
    return false;
  } else {
    const sid = process.env.TWILIO_ACCOUNT_SID, token = process.env.TWILIO_AUTH_TOKEN, from = process.env.TWILIO_FROM_PHONE;
    if (!sid || !token) throw new ServiceUnavailableException('SMS delivery is not configured');
    const authorization = `Basic ${Buffer.from(`${sid}:${token}`).toString('base64')}`;
    if (process.env.TWILIO_VERIFY_SERVICE_SID) {
      const response = await fetch(`https://verify.twilio.com/v2/Services/${process.env.TWILIO_VERIFY_SERVICE_SID}/Verifications`, {
        method: 'POST', headers: { Authorization: authorization, 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ To: id, Channel: 'sms' }),
      });
      if (!response.ok) throw new ServiceUnavailableException('SMS verification delivery failed');
      return true;
    }
    if (!from) throw new ServiceUnavailableException('SMS delivery is not configured');
    const body = new URLSearchParams({ To: id, From: from, Body: `Your Vox signup code is ${code}. It expires in 10 minutes.` });
    const response = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${sid}/Messages.json`, { method: 'POST', headers: { Authorization: authorization, 'Content-Type': 'application/x-www-form-urlencoded' }, body });
    if (!response.ok) throw new ServiceUnavailableException('SMS delivery failed');
    return false;
  }
}
async function verifyPhone(id: string, code: string) {
  const sid = process.env.TWILIO_ACCOUNT_SID, token = process.env.TWILIO_AUTH_TOKEN, service = process.env.TWILIO_VERIFY_SERVICE_SID;
  if (!sid || !token || !service) throw new ServiceUnavailableException('SMS verification is not configured');
  const response = await fetch(`https://verify.twilio.com/v2/Services/${service}/VerificationCheck`, {
    method: 'POST', headers: { Authorization: `Basic ${Buffer.from(`${sid}:${token}`).toString('base64')}`, 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ To: id, Code: code }),
  });
  if (!response.ok || (await response.json() as { status?: string }).status !== 'approved') throw new UnauthorizedException('Invalid or expired code');
}
class Profile {
  @IsOptional() @IsString() @Length(1, 100) name?: string;
  @IsOptional() @Matches(/^[A-Z]{3}$/) currencyCode?: string;
  @IsOptional() @IsString() @Length(1, 30) phone?: string;
  @IsOptional() @IsString() @Length(1, 80) timeZone?: string;
  @IsOptional() @IsString() @Length(1, 30) locale?: string;
}
@Controller('auth')
export class AuthController {
  constructor(private db: Db) {}
  private async session(user: any) {
    const token = randomBytes(32).toString('base64url');
    await this.db.session.create({ data: { userId: user.id, tokenHash: hash(token), expiresAt: new Date(Date.now() + 30 * 86400_000) } });
    return { token, user: publicUser(user) };
  }
  private async checkCode(identifier: string, code: string) {
    const claimed = await this.db.signupCode.updateMany({ where: { identifier, expiresAt: { gt: new Date() }, attempts: { lt: 5 } }, data: { attempts: { increment: 1 } } });
    if (!claimed.count) throw new UnauthorizedException('Invalid or expired code');
    const row = await this.db.signupCode.findUniqueOrThrow({ where: { identifier } });
    if (!identifier.includes('@') && !row.codeHash) {
      await verifyPhone(identifier, code);
      await this.db.signupCode.update({ where: { identifier }, data: { codeHash: passwordHash(code) } });
      return;
    }
    if (!passwordMatches(code, row.codeHash)) throw new UnauthorizedException('Invalid or expired code');
  }
  @Post('signup/start') async start(@Body() body: SignupStart) {
    const identifier = contact(body.identifier);
    const existing = await this.db.user.findFirst({ where: identifier.includes('@') ? { email: identifier } : { phone: identifier } });
    if (existing) throw new ConflictException('Account already registered');
    const previous = await this.db.signupCode.findUnique({ where: { identifier } });
    if (previous && Date.now() - previous.createdAt.getTime() < 60_000) throw new HttpException('Wait before requesting another code', HttpStatus.TOO_MANY_REQUESTS);
    const code = String(randomInt(0, 1_000_000)).padStart(6, '0');
    const providerGenerated = await deliver(identifier, code);
    const codeHash = providerGenerated ? '' : passwordHash(code);
    await this.db.signupCode.upsert({ where: { identifier }, create: { identifier, codeHash, expiresAt: new Date(Date.now() + 600_000) }, update: { codeHash, expiresAt: new Date(Date.now() + 600_000), attempts: 0, createdAt: new Date() } });
    return { ok: true };
  }
  @Post('signup/verify') async verify(@Body() body: SignupVerify) {
    await this.checkCode(contact(body.identifier), body.code);
    return { ok: true };
  }
  @Post('register') async register(@Body() body: SignupFinish) {
    const identifier = contact(body.identifier);
    if (body.password !== body.confirmPassword) throw new BadRequestException('Passwords do not match');
    await this.checkCode(identifier, body.code);
    const existing = await this.db.user.findFirst({ where: identifier.includes('@') ? { email: identifier } : { phone: identifier } });
    if (existing) throw new ConflictException('Account already registered');
    const user = await this.db.user.create({ data: { email: identifier.includes('@') ? identifier : null, phone: identifier.includes('@') ? null : identifier, passwordHash: passwordHash(body.password), name: body.name || '' } });
    await this.db.signupCode.delete({ where: { identifier } });
    return this.session(user);
  }
  @Post('login') async login(@Body() body: Credentials) {
    const identifier = contact(body.identifier);
    const user = await this.db.user.findFirst({ where: identifier.includes('@') ? { email: identifier } : { phone: identifier } });
    if (!user || !passwordMatches(body.password, user.passwordHash)) throw new UnauthorizedException('Invalid credentials');
    return this.session(user);
  }
  @UseGuards(AuthGuard) @Post('refresh') async refresh(@Req() req: any) {
    await this.db.session.delete({ where: { id: req.session.id } });
    return this.session(req.user);
  }
  @UseGuards(AuthGuard) @Post('logout') async logout(@Req() req: any) {
    await this.db.session.delete({ where: { id: req.session.id } });
    return { ok: true };
  }
}
@UseGuards(AuthGuard) @Controller('users')
export class UsersController {
  constructor(private db: Db) {}
  @Get('me') me(@Req() req: any) { return publicUser(req.user); }
  @Put('me') async update(@Req() req: any, @Body() body: Profile) {
    if (body.phone !== undefined && body.phone !== req.user.phone) throw new BadRequestException('Phone changes require verification');
    return publicUser(await this.db.user.update({ where: { id: req.user.id }, data: body }));
  }
  @Delete('me') async remove(@Req() req: any) {
    await this.db.user.delete({ where: { id: req.user.id } });
    return { ok: true };
  }
}
