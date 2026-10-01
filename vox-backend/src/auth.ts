import { Body, Controller, Delete, Get, Post, Put, Req, UseGuards, BadRequestException, ConflictException, UnauthorizedException } from '@nestjs/common';
import { IsEmail, IsOptional, IsString, Length, Matches } from 'class-validator';
import { Db, AuthGuard, hash, passwordHash, passwordMatches, publicUser } from './core';
import { randomBytes } from 'node:crypto';

class Credentials {
  @IsEmail() email!: string;
  @IsString() @Length(8, 128) password!: string;
  @IsOptional() @IsString() @Length(1, 100) name?: string;
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
  @Post('register') async register(@Body() body: Credentials) {
    const email = body.email.toLowerCase().trim();
    if (await this.db.user.findUnique({ where: { email } })) throw new ConflictException('Email already registered');
    const user = await this.db.user.create({ data: { email, passwordHash: passwordHash(body.password), name: body.name || '' } });
    return this.session(user);
  }
  @Post('login') async login(@Body() body: Credentials) {
    const user = await this.db.user.findUnique({ where: { email: body.email.toLowerCase().trim() } });
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
    return publicUser(await this.db.user.update({ where: { id: req.user.id }, data: body }));
  }
  @Delete('me') async remove(@Req() req: any) {
    await this.db.user.delete({ where: { id: req.user.id } });
    return { ok: true };
  }
}
