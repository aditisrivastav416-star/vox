import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './module';
import type { Request, Response, NextFunction } from 'express';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { bodyParser: false });
  app.use(require('express').json({ limit: '8mb' }));
  app.use((req: Request, res: Response, next: NextFunction) => {
    if (req.method === 'GET' && req.path === '/') {
      return res.json({ service: 'Vox API', health: '/api/v1/health' });
    }
    next();
  });
  app.setGlobalPrefix('api/v1');
  await app.listen(Number(process.env.PORT || 3000));
}
bootstrap();
