import {
  Controller,
  Get,
  Post,
  Body,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { HistoryService, HistoryItem } from './history.service';

@Controller('history')
export class HistoryController {
  constructor(private readonly historyService: HistoryService) {}

  @Get()
  async getHistory(): Promise<HistoryItem[]> {
    return this.historyService.getHistory();
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  async addHistory(
    @Body()
    payload: {
      type: 'hoax' | 'scam';
      title: string;
      score: number;
      status: string;
      resultDetails: any;
    },
  ): Promise<HistoryItem> {
    return this.historyService.addHistory(
      payload.type,
      payload.title,
      payload.score,
      payload.status,
      payload.resultDetails,
    );
  }

  @Post('clear')
  @HttpCode(HttpStatus.OK)
  async clearHistory(): Promise<{ success: boolean }> {
    const success = await this.historyService.clearHistory();
    return { success };
  }
}
