import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { bodyParser: false });
  app.use(require('express').json({ limit: '8mb' }));
  app.setGlobalPrefix('api/v1');
  await app.listen(Number(process.env.PORT || 3000));
}
bootstrap();
