import { BadGatewayException, BadRequestException, Controller, Get, Module, Post, Body, ServiceUnavailableException } from '@nestjs/common';

@Controller()
class VoiceController {
  @Get('health') health() { return { ok: true }; }

  @Post('voice/speak')
  async speak(@Body() body: { text?: string; languageCode?: string }) {
    const key = process.env.GNANI_API_KEY;
    if (!key) throw new ServiceUnavailableException('GNANI_API_KEY is not configured');
    const spokenText = body?.text?.trim();
    if (!spokenText || spokenText.length > 500) throw new BadRequestException('Text must be 1–500 characters');
    const language = body.languageCode || 'en-IN';
    if (!['en-IN', 'hi-IN'].includes(language)) throw new BadRequestException('Unsupported reply language');
    const response = await fetch('https://api.vachana.ai/api/v1/tts/inference', {
      method: 'POST',
      headers: { 'X-API-Key-ID': key, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text: spokenText, voice: 'Nalini', model: 'timbre-v2.5', language,
        audio_config: { container: 'mp3', sample_rate: 24000, num_channels: 1 },
      }),
      signal: AbortSignal.timeout(25_000),
    }).catch(() => { throw new BadGatewayException('Gnani is unavailable'); });
    if (!response.ok) throw new BadGatewayException(`Gnani returned ${response.status}`);
    const audio = Buffer.from(await response.arrayBuffer());
    if (!audio.length) throw new BadGatewayException('Gnani returned no audio');
    return { audioBase64: audio.toString('base64') };
  }

  @Post('voice/transcribe')
  async transcribe(@Body() body: { audioBase64?: string; languageCode?: string }) {
    const key = process.env.GNANI_API_KEY;
    if (!key) throw new ServiceUnavailableException('GNANI_API_KEY is not configured');
    const language = body?.languageCode || 'en-IN';
    if (!/^(en|hi|bn|gu|kn|ml|mr|pa|ta|te)-IN$/.test(language)) throw new BadRequestException('Unsupported language');
    if (!body?.audioBase64 || !/^[A-Za-z0-9+/]+={0,2}$/.test(body.audioBase64) || body.audioBase64.length > 8_000_000) {
      throw new BadRequestException('Invalid or oversized audio');
    }
    const audio = Buffer.from(body.audioBase64, 'base64');
    if (!audio.length) throw new BadRequestException('Audio is empty');
    const mp3 = audio.subarray(0, 3).toString() === 'ID3' || (audio[0] === 0xff && (audio[1] & 0xe0) === 0xe0);
    const form = new FormData();
    form.append('audio_file', new Blob([audio], { type: mp3 ? 'audio/mpeg' : 'audio/mp4' }), mp3 ? 'recording.mp3' : 'recording.m4a');
    form.append('language_code', language);
    form.append('format', 'transcribe');
    const response = await fetch('https://api.vachana.ai/stt/v3', {
      method: 'POST', headers: { 'X-API-Key-ID': key }, body: form, signal: AbortSignal.timeout(25_000),
    }).catch(() => { throw new BadGatewayException('Gnani is unavailable'); });
    const result = await response.json().catch(() => null) as { success?: boolean; transcript?: string; error?: { message?: string } } | null;
    if (!response.ok || result?.success === false) {
      throw new BadGatewayException(result?.error?.message?.slice(0, 200) || `Gnani STT returned ${response.status}`);
    }
    if (!result?.transcript?.trim()) throw new BadRequestException('No speech detected. Check microphone input and try again.');
    return { transcript: result.transcript.trim() };
  }
}

@Module({ controllers: [VoiceController] })
export class AppModule {}
