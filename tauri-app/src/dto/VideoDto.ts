import { LiveStatusEnum } from "../enums/LiveStatusEnum";

export interface VideoDto {
    pkid?: number;
    name?: string;
    videoPath?: string;
    extension?: string;
    liveStatus?: LiveStatusEnum;
    lastTimeStampBeforeStop?: number;
    message?: string;
    shouldBeStop?: boolean;
    startDateLive?: Date;
    channelName?: string;
}
