namespace Orbis.Stream.Core.Domain;

/// <summary>Maps the JavaCV/FFmpeg numeric identifiers stored in the database to ffmpeg names.</summary>
public static class FfmpegCodecCatalog
{
    // org.bytedeco.javacv.FFmpegFrameRecorder AV_CODEC_ID_* values.
    private static readonly Dictionary<int, string> VideoCodecs = new()
    {
        [0] = "mpeg1video",
        [1] = "mpeg2video",
        [2] = "h261",
        [3] = "h263",
        [4] = "rv10",
        [5] = "rv20",
        [6] = "mjpeg",
        [7] = "mjpegb",
        [8] = "ljpeg",
        [9] = "sp5x",
        [10] = "jpegls",
        [12] = "wmv1",
        [13] = "wmv2",
        [14] = "vc1",
        [16] = "h264",
        [17] = "h264",
        [18] = "mpeg4",
        [19] = "mpeg4",
        [20] = "h263p",
        [25] = "theora",
        [26] = "mpeg2video",
        [27] = "h264",
        [28] = "libvpx",
        [30] = "libtheora",
        [31] = "h264",
        [32] = "libxvid",
        [33] = "mpeg4",
        [39] = "libxvid",
        [40] = "libx264",
        [46] = "flv",
        [69] = "mpeg4",
        [71] = "vp3",
        [73] = "vp6f",
        [85] = "vp6a",
        [92] = "wmv3",
        [121] = "libvpx-vp9",
        [173] = "libaom-av1",
        [176] = "h264",
        [177] = "hevc",
        [178] = "hevc",
        [192] = "vp9",
        [225] = "prores"
    };

    // AV_CODEC_ID_AAC and the other audio identifiers used by the shipped defaults.
    private static readonly Dictionary<int, string> AudioCodecs = new()
    {
        [0x11000] = "mp3",
        [86018] = "aac",
        [86019] = "aac",
        [86020] = "ac3",
        [86021] = "eac3",
        [86023] = "vorbis",
        [86026] = "flac",
        [86028] = "libopus",
        [86033] = "pcm_s16le",
        [86037] = "alac"
    };

    // AV_PIX_FMT_* values that can be expressed as an ffmpeg -pix_fmt name.
    private static readonly Dictionary<int, string> PixelFormats = new()
    {
        [0] = "yuv420p",
        [1] = "rgb24",
        [2] = "bgr24",
        [3] = "yuv422p",
        [4] = "yuv444p",
        [5] = "yuyv422",
        [6] = "yvyu422",
        [7] = "uyvy422",
        [8] = "gray",
        [9] = "monob",
        [10] = "monow",
        [11] = "pal8",
        [12] = "yuv420p",
        [13] = "yuv422p",
        [14] = "yuv444p",
        [15] = "yuv410p",
        [16] = "yuv411p",
        [19] = "rgb565",
        [20] = "rgb555",
        [21] = "bgr565",
        [23] = "nv12",
        [25] = "nv21",
        [26] = "yuyv422",
        [27] = "uyvy422",
        [30] = "yuv440p",
        [33] = "yuvj422p",
        [34] = "yuvj444p",
        [35] = "yuva420p",
        [36] = "yuva422p",
        [37] = "yuva444p",
        [38] = "argb",
        [39] = "rgba",
        [40] = "abgr",
        [41] = "bgra",
        [42] = "0rgb",
        [43] = "rgb0",
        [44] = "bgr0",
        [45] = "0bgr"
    };

    public static string ResolveVideoCodecName(int? videoCodec, string? videoCodecName)
    {
        if (!string.IsNullOrWhiteSpace(videoCodecName))
        {
            return videoCodecName.Trim();
        }

        if (videoCodec is not null && VideoCodecs.TryGetValue(videoCodec.Value, out var name))
        {
            return name;
        }

        return "libx264";
    }

    public static string ResolveAudioCodecName(int? audioCodec, string? audioCodecName = null)
    {
        if (!string.IsNullOrWhiteSpace(audioCodecName))
        {
            return audioCodecName.Trim();
        }

        if (audioCodec is not null && AudioCodecs.TryGetValue(audioCodec.Value, out var name))
        {
            return name;
        }

        return "aac";
    }

    public static string ResolvePixelFormat(int? pixelFormat)
    {
        if (pixelFormat is not null && PixelFormats.TryGetValue(pixelFormat.Value, out var name))
        {
            return name;
        }

        return "yuv420p";
    }
}
