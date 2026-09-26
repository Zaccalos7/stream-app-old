using System.IO;
using System.Windows;
using Microsoft.AspNetCore.Builder;
using Microsoft.Web.WebView2.Core;
using Orbis.Stream.Core.Configuration;

namespace Orbis.Stream.App;

/// <summary>
/// WebView2 host window: it shows the Razor pages served by the in-process Kestrel host,
/// replacing the JCEF window of the Java version.
/// </summary>
public partial class MainWindow : Window
{
    private readonly OrbisRuntimeOptions _options;
    private readonly WebApplication _host;

    public MainWindow(OrbisRuntimeOptions options, WebApplication host)
    {
        _options = options;
        _host = host;

        InitializeComponent();
        Loaded += OnLoaded;
        Closing += OnClosing;
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        try
        {
            var userDataFolder = Path.Combine(_options.DataDirectory, "webview2");
            Directory.CreateDirectory(userDataFolder);

            var environment = await CoreWebView2Environment.CreateAsync(userDataFolder: userDataFolder)
                .ConfigureAwait(true);

            await Browser.EnsureCoreWebView2Async(environment).ConfigureAwait(true);

            Browser.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
            Browser.CoreWebView2.Settings.AreDevToolsEnabled = true;
            Browser.CoreWebView2.Settings.IsStatusBarEnabled = false;
            Browser.CoreWebView2.SetVirtualHostNameToFolderMapping(
                "orbis.local",
                _options.WebRootPath,
                CoreWebView2HostResourceAccessKind.Allow);
            Browser.CoreWebView2.Navigate(_options.ApplicationEntryPoint);
            Browser.CoreWebView2.NavigationCompleted += OnNavigationCompleted;
        }
        catch (Exception exception)
        {
            StatusText.Text = $"Impossibile inizializzare il browser: {exception.Message}";
        }
    }

    private void OnNavigationCompleted(object? sender, CoreWebView2NavigationCompletedEventArgs e)
    {
        StatusText.Visibility = e.IsSuccess ? Visibility.Collapsed : Visibility.Visible;
        if (!e.IsSuccess)
        {
            StatusText.Text = $"Il browser non riesce a raggiungere {_options.ApplicationUrl}.";
        }
    }

    private void OnClosing(object? sender, System.ComponentModel.CancelEventArgs e)
    {
        Browser.CoreWebView2?.Stop();
    }
}
