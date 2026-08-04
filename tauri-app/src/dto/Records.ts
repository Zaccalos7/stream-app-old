import { LiveStatusEnum } from '../enums/LiveStatusEnum';

export interface VideoLiveHistoryRecord {
    pkid?: number;
    folderOfVideoToStream?: string;
    localDateTimeStartLive?: Date;
    streamUrl?: string;
    streamKey?: string;
    platformStreamName?: string;
    userName?: string;
}

export interface VideoSettingsOptionRecord {
    id?: number;
    key?: string;
    value?: string;
}

export interface AudioSettingsRecord {
    id?: number;
    audioCodec?: number;
    audioBitrate?: number;
}

export interface VideoSettingsRecord {
    id?: number;
    title?: string;
    videoCodec?: number;
    videoCodecName?: string;
    pixelFormat?: number;
    videoBitrate?: number;
    videoFormat?: string;
    lastModified?: Date;
    gopSize?: number;
    isVideoAndAudioSettingActive?: boolean;
    videoSettingsOptions?: VideoSettingsOptionRecord[];
    audioSetting?: AudioSettingsRecord;
}

export interface VideoRecord {
    pkid?: number;
    name?: string;
    videoPath?: string;
    extension?: string;
    liveStatus?: LiveStatusEnum;
    lastTimeStampBeforeStop?: number;
    message?: string;
    videoLiveHistory?: VideoLiveHistoryRecord;
    videoSetting?: VideoSettingsRecord;
    shouldBeStop?: boolean;
    startDateLive?: Date;
    channelName?: string;
}

export interface VideoPathAndKey {
    videoPath?: string;
    videoKey?: number;
}

export interface ChannelRecord {
    channelName?: string;
    videoPathAndKeyList?: VideoPathAndKey[];
}

export interface StartLiveRecord {
    channelName?: string;
    platformStreamName?: string;
    videoPath?: string;
    streamKey?: string;
    streamUrl?: string;
    videoSettingsRecord?: VideoSettingsRecord;
}
