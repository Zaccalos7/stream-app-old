using System.Windows;
using System.Windows.Threading;
using Microsoft.AspNetCore.Builder;
using Orbis.Stream.Core.Configuration;
using Orbis.Stream.Core.Hosting;
using Orbis.Stream.Core.Services;
using Orbis.Stream.Core.Streaming;

namespace Orbis.Stream.App;

/// <summary>
/// Desktop shell of the ported application: it starts the ASP.NET Core host in-process and
/// shows the unchanged React build inside WebView2, exactly like the JCEF window did.
/// </summary>
public partial class App : Application
{
    private WebApplication? _host;

    private async void OnStartup(object sender, StartupEventArgs e)
    {
        base.OnStartup(e);

        DispatcherUnhandledException += OnDispatcherUnhandledException;

        OrbisRuntimeOptions options;
        try
        {
            options = OrbisHost.ResolveOptions(e.Args);
            _host = OrbisHost.Create(options, e.Args);
            await _host.StartAsync().ConfigureAwait(true);
        }
        catch (Exception exception)
        {
            MessageBox.Show(
                $"Impossibile avviare Orbis Stream:{Environment.NewLine}{exception.Message}",
                "Orbis Stream",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
            Shutdown(1);
            return;
        }

        if (!options.EmbeddedBrowser)
        {
            // Same as LAZY_STREAM_EMBEDDED_BROWSER=false: the server keeps running without a window.
            return;
        }

        var window = new MainWindow(options, _host);
        MainWindow = window;
        window.Show();
    }

    protected override async void OnExit(ExitEventArgs e)
    {
        await ShutdownHostAsync().ConfigureAwait(true);
        base.OnExit(e);
    }

    private async Task ShutdownHostAsync()
    {
        if (_host is null)
        {
            return;
        }

        try
        {
            if (_host.Services.GetService(typeof(StreamingSessionRegistry)) is StreamingSessionRegistry sessions)
            {
                await sessions.StopAllAsync().ConfigureAwait(false);
            }

            if (_host.Services.GetService(typeof(StreamingService)) is StreamingService streaming)
            {
                streaming.Shutdown();
            }

            if (_host.Services.GetService(typeof(BackgroundTaskExecutor)) is BackgroundTaskExecutor executor)
            {
                await executor.DisposeAsync().ConfigureAwait(false);
            }

            using var shutdown = new CancellationTokenSource(TimeSpan.FromSeconds(5));
            await _host.StopAsync(shutdown.Token).ConfigureAwait(false);
        }
        catch (Exception exception)
        {
            System.Diagnostics.Debug.WriteLine($"Shutdown error: {exception}");
        }
        finally
        {
            await _host.DisposeAsync().ConfigureAwait(true);
            _host = null;
        }
    }

    private void OnDispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
    {
        MessageBox.Show(
            $"Errore inatteso:{Environment.NewLine}{e.Exception.Message}",
            "Orbis Stream",
            MessageBoxButton.OK,
            MessageBoxImage.Error);
        e.Handled = true;
    }
}
