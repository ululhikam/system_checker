import {
  Controller,
  Post,
  Body,
  UseInterceptors,
  UploadedFile,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ScamService, UrlScanRequest, ScamScanResult } from './scam.service';

@Controller()
export class ScamController {
  constructor(private readonly scamService: ScamService) {}

  @Post('scan-url')
  @HttpCode(HttpStatus.OK)
  async scanUrl(@Body() payload: UrlScanRequest): Promise<ScamScanResult> {
    return this.scamService.scanUrl(payload);
  }

  @Post('scan-file')
  @HttpCode(HttpStatus.OK)
  @UseInterceptors(FileInterceptor('file'))
  async scanFile(
    @UploadedFile() file: any,
    @Body('fileName') bodyFileName?: string,
    @Body('fileSize') bodyFileSize?: string,
  ): Promise<ScamScanResult> {
    const fileName = file
      ? file.originalname
      : bodyFileName || 'unknown_file.bin';
    const fileSize = file
      ? file.size
      : bodyFileSize
        ? parseInt(bodyFileSize)
        : 1024;
    const fileBase64 = file ? file.buffer.toString('base64') : undefined;

    return this.scamService.scanFile({
      fileName,
      fileSize,
      fileBase64,
    });
  }
}
