using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Orbis.Stream.Core.Contracts;
using Orbis.Stream.Core.Data;
using Orbis.Stream.Core.Domain;
using Orbis.Stream.Core.Http;
using Orbis.Stream.Core.I18n;

namespace Orbis.Stream.Core.Services;

/// <summary>Port of <c>com.orbis.stream.service.VideoService</c>.</summary>
public sealed class VideoService
{
    private readonly VideoRepository _videoRepository;
    private readonly ResponseFactory _responses;
    private readonly Localizer _localizer;
    private readonly BackgroundTaskExecutor _executor;
    private readonly ILogger<VideoService> _logger;

    public VideoService(
        VideoRepository videoRepository,
        ResponseFactory responses,
        Localizer localizer,
        BackgroundTaskExecutor executor,
        ILogger<VideoService> logger)
    {
        _videoRepository = videoRepository;
        _responses = responses;
        _localizer = localizer;
        _executor = executor;
        _logger = logger;
    }

    public SpringPage<VideoResponse> GetVideoList(PageRequest page)
    {
        var result = _videoRepository.FindPaged(new Dictionary<string, string>(), page);
        _logger.LogInformation("{Message}", _localizer.PrintMessage("recovered.video.page"));
        return SpringPageFactory.Create(Map(result, VideoResponse.FromEntity), page.Sorts);
    }

    public SpringPage<VideoRequest> GetAllVideoList(IReadOnlyDictionary<string, string> filters, PageRequest page)
    {
        var result = _videoRepository.FindPaged(filters, page);
        _logger.LogInformation("{Message}", _localizer.PrintMessage("recovered.video"));
        return SpringPageFactory.Create(Map(result, VideoRequest.FromEntity), page.Sorts);
    }

    private static PagedResult<TTarget> Map<TSource, TTarget>(
        PagedResult<TSource> source,
        Func<TSource, TTarget> projection) => new(
        source.Items.Select(projection).ToList(),
        source.Page,
        source.Size,
        source.TotalElements);

    /// <summary>All channels with the live videos currently streaming, as the stop card needs.</summary>
    public List<ChannelResponse> GetAllChannelOnline()
    {
        var videoList = _videoRepository.FindByLiveStatus(LiveStatus.Live);
        if (videoList.Count == 0)
        {
            _logger.LogError("{Message}", _localizer.PrintMessage("nothing.to.stop"));
            throw new NotFoundCustomException("nothing.to.stop");
        }

        _logger.LogInformation("found video to stop");

        return videoList
            .Select(video => video.ChannelName)
            .Distinct()
            .Select(channelName => new ChannelResponse(
                channelName,
                videoList
                    .Where(video => string.Equals(video.ChannelName, channelName, StringComparison.Ordinal))
                    .Select(video => new VideoPathAndKeyResponse(video.VideoPath, video.Pkid))
                    .ToList()))
            .ToList();
    }

    /// <summary>Port of <c>unlockVideo</c>: asks the stream to stop and forces it offline after 3 checks.</summary>
    public MessageResponse UnlockVideo(int videoKey)
    {
        var video = FindVideoToUnlock(videoKey);
        _executor.Execute(() => _ = UnlockAsync(video));

        return _responses.Build("unlock.in.progress", StatusCodes.Status202Accepted);
    }

    private async Task UnlockAsync(VideoEntity video)
    {
        video.ShouldBeStop = true;
        _videoRepository.Update(video);

        if (await CheckThreeTimesAsync(video.Pkid).ConfigureAwait(false))
        {
            return;
        }

        video.LiveStatus = LiveStatus.Offline;
        _videoRepository.Update(video);
    }

    private async Task<bool> CheckThreeTimesAsync(int videoPkid)
    {
        for (var attempt = 0; attempt < 3; attempt++)
        {
            if (CheckIfVideoGoOffline(videoPkid))
            {
                return true;
            }

            await Task.Delay(TimeSpan.FromSeconds(1)).ConfigureAwait(false);
        }

        return false;
    }

    private bool CheckIfVideoGoOffline(int videoPkid)
    {
        var video = _videoRepository.FindByPkid(videoPkid);
        if (video is null)
        {
            _logger.LogError("{Message}", _localizer.PrintMessage("video.not.found"));
            throw new NotFoundCustomException("video.not.found");
        }

        return video.LiveStatus == LiveStatus.Offline;
    }

    private VideoEntity FindVideoToUnlock(int videoKey)
    {
        var video = _videoRepository.FindByPkid(videoKey);
        if (video is null)
        {
            _logger.LogError("{Message}", _localizer.PrintMessage("video.not.found"));
            throw new NotFoundCustomException("video.not.found");
        }

        return video;
    }
}
