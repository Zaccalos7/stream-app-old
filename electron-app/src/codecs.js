const VIDEO_CODECS = [
  { id: 2, name: 'MPEG2VIDEO', ffmpeg: 'mpeg2video' },
  { id: 4, name: 'H263', ffmpeg: 'h263' },
  { id: 7, name: 'MJPEG', ffmpeg: 'mjpeg' },
  { id: 12, name: 'MPEG4', ffmpeg: 'mpeg4' },
  { id: 21, name: 'FLV1', ffmpeg: 'flv' },
  { id: 24, name: 'DVVIDEO', ffmpeg: 'dvvideo' },
  { id: 25, name: 'HUFFYUV', ffmpeg: 'huffyuv' },
  { id: 27, name: 'H264', ffmpeg: 'libx264' },
  { id: 28, name: 'INDEO3', ffmpeg: 'indeo3' },
  { id: 30, name: 'THEORA', ffmpeg: 'libtheora' },
  { id: 43, name: 'CINEPAK', ffmpeg: 'cinepak' },
  { id: 70, name: 'VC1', ffmpeg: 'vc1' },
  { id: 71, name: 'WMV3', ffmpeg: 'wmv3' },
  { id: 91, name: 'VP6', ffmpeg: 'vp6' },
  { id: 92, name: 'VP6F', ffmpeg: 'vp6f' },
  { id: 99, name: 'DNXHD', ffmpeg: 'dnxhd' },
  { id: 116, name: 'DIRAC', ffmpeg: 'dirac' },
  { id: 139, name: 'VP8', ffmpeg: 'libvpx' },
  { id: 147, name: 'PRORES', ffmpeg: 'prores' },
  { id: 167, name: 'VP9', ffmpeg: 'libvpx-vp9' },
  { id: 173, name: 'H265', ffmpeg: 'libx265' },
  { id: 225, name: 'AV1', ffmpeg: 'libaom-av1' },
];

const AUDIO_CODECS = [
  { id: 86016, name: 'MP2', ffmpeg: 'mp2' },
  { id: 86017, name: 'MP3', ffmpeg: 'libmp3lame' },
  { id: 86018, name: 'AAC', ffmpeg: 'aac' },
  { id: 86019, name: 'AC3', ffmpeg: 'ac3' },
  { id: 86020, name: 'DTS', ffmpeg: 'dca' },
  { id: 86028, name: 'FLAC', ffmpeg: 'flac' },
  { id: 86032, name: 'ALAC', ffmpeg: 'alac' },
  { id: 86045, name: 'MLP', ffmpeg: 'mlp' },
  { id: 86048, name: 'APE', ffmpeg: 'ape' },
  { id: 73728, name: 'AMR-NB', ffmpeg: 'amrnb' },
  { id: 73729, name: 'AMR-WB', ffmpeg: 'amrwb' },
];

const PIXEL_FORMATS = [
  { id: 0, name: 'YUV420P', ffmpeg: 'yuv420p' },
  { id: 1, name: 'YUYV422', ffmpeg: 'yuyv422' },
  { id: 2, name: 'RGB24', ffmpeg: 'rgb24' },
  { id: 3, name: 'BGR24', ffmpeg: 'bgr24' },
  { id: 4, name: 'YUV422P', ffmpeg: 'yuv422p' },
  { id: 5, name: 'YUV444P', ffmpeg: 'yuv444p' },
  { id: 6, name: 'YUV410P', ffmpeg: 'yuv410p' },
  { id: 7, name: 'YUV411P', ffmpeg: 'yuv411p' },
  { id: 8, name: 'GRAY8', ffmpeg: 'gray' },
  { id: 9, name: 'MONOWHITE', ffmpeg: 'monow' },
  { id: 10, name: 'MONOBLACK', ffmpeg: 'monob' },
  { id: 11, name: 'PAL8', ffmpeg: 'pal8' },
  { id: 12, name: 'YUVJ420P', ffmpeg: 'yuvj420p' },
  { id: 13, name: 'YUVJ422P', ffmpeg: 'yuvj422p' },
  { id: 14, name: 'YUVJ444P', ffmpeg: 'yuvj444p' },
  { id: 17, name: 'BGR8', ffmpeg: 'bgr8' },
  { id: 18, name: 'BGR4', ffmpeg: 'bgr4' },
  { id: 19, name: 'BGR4_BYTE', ffmpeg: 'bgr4_byte' },
  { id: 20, name: 'RGB8', ffmpeg: 'rgb8' },
  { id: 21, name: 'RGB4', ffmpeg: 'rgb4' },
  { id: 22, name: 'RGB4_BYTE', ffmpeg: 'rgb4_byte' },
  { id: 23, name: 'NV12', ffmpeg: 'nv12' },
  { id: 25, name: 'ARGB', ffmpeg: 'argb' },
  { id: 26, name: 'RGBA', ffmpeg: 'rgba' },
  { id: 27, name: 'ABGR', ffmpeg: 'abgr' },
  { id: 28, name: 'BGRA', ffmpeg: 'bgra' },
  { id: 29, name: 'GRAY16BE', ffmpeg: 'gray16be' },
  { id: 30, name: 'GRAY16LE', ffmpeg: 'gray16le' },
  { id: 31, name: 'YUV440P', ffmpeg: 'yuv440p' },
  { id: 32, name: 'YUVJ440P', ffmpeg: 'yuvj440p' },
  { id: 33, name: 'YUVA420P', ffmpeg: 'yuva420p' },
  { id: 34, name: 'RGB48BE', ffmpeg: 'rgb48be' },
  { id: 35, name: 'RGB48LE', ffmpeg: 'rgb48le' },
  { id: 36, name: 'RGB565BE', ffmpeg: 'rgb565be' },
  { id: 37, name: 'RGB565LE', ffmpeg: 'rgb565le' },
  { id: 38, name: 'RGB555BE', ffmpeg: 'rgb555be' },
  { id: 39, name: 'RGB555LE', ffmpeg: 'rgb555le' },
  { id: 40, name: 'BGR565BE', ffmpeg: 'bgr565be' },
  { id: 41, name: 'BGR565LE', ffmpeg: 'bgr565le' },
  { id: 42, name: 'BGR555BE', ffmpeg: 'bgr555be' },
  { id: 43, name: 'BGR555LE', ffmpeg: 'bgr555le' },
  { id: 44, name: 'VAAPI', ffmpeg: 'vaapi' },
  { id: 45, name: 'YUV420P16LE', ffmpeg: 'yuv420p16le' },
  { id: 46, name: 'YUV420P16BE', ffmpeg: 'yuv420p16be' },
  { id: 47, name: 'YUV422P16LE', ffmpeg: 'yuv422p16le' },
  { id: 48, name: 'YUV422P16BE', ffmpeg: 'yuv422p16be' },
  { id: 49, name: 'YUV444P16LE', ffmpeg: 'yuv444p16le' },
  { id: 50, name: 'YUV444P16BE', ffmpeg: 'yuv444p16be' },
  { id: 52, name: 'RGB444LE', ffmpeg: 'rgb444le' },
  { id: 53, name: 'RGB444BE', ffmpeg: 'rgb444be' },
  { id: 54, name: 'BGR444LE', ffmpeg: 'bgr444le' },
  { id: 55, name: 'BGR444BE', ffmpeg: 'bgr444be' },
];

const videoCodecById = new Map(VIDEO_CODECS.map((c) => [c.id, c]));
const audioCodecById = new Map(AUDIO_CODECS.map((c) => [c.id, c]));
const pixelFormatById = new Map(PIXEL_FORMATS.map((c) => [c.id, c]));

function resolveVideoCodec(videoCodecId, videoCodecName) {
  if (videoCodecName && typeof videoCodecName === 'string' && videoCodecName.trim()) {
    const name = videoCodecName.trim().toLowerCase();
    if (name === 'nvenc' || name === 'h264_nvenc') {
      return 'h264_nvenc';
    }
    if (name === 'hevc_nvenc' || name === 'nvenc_hevc') {
      return 'hevc_nvenc';
    }
    return videoCodecName.trim();
  }
  const entry = videoCodecById.get(Number(videoCodecId));
  return entry ? entry.ffmpeg : undefined;
}

function resolveAudioCodec(audioCodecId) {
  const entry = audioCodecById.get(Number(audioCodecId));
  return entry ? entry.ffmpeg : undefined;
}

function resolvePixelFormat(pixelFormatId) {
  const entry = pixelFormatById.get(Number(pixelFormatId));
  return entry ? entry.ffmpeg : undefined;
}

module.exports = {
  VIDEO_CODECS,
  AUDIO_CODECS,
  PIXEL_FORMATS,
  resolveVideoCodec,
  resolveAudioCodec,
  resolvePixelFormat,
};
