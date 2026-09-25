using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.FileProviders;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.OpenApi;
using Microsoft.OpenApi.Extensions;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.Swagger;
using Orbis.Stream.Core.Configuration;
using Orbis.Stream.Core.Contracts;
using Orbis.Stream.Core.Http;

namespace Orbis.Stream.Core.Hosting;

/// <summary>
/// Wires the Kestrel host, the React static resources, the exception middleware and the OpenAPI
/// document at the same paths the Spring application exposed.
/// </summary>
public static class OrbisWebApplicationExtensions
{
    public const string OpenApiDocumentPath = "/documentazione";
    public const string OpenApiDocumentName = "v1";
    public const string SwaggerUiPath = "/swagger-ui.html";

    /// <summary>Spring Boot <c>server.port</c>: every interface, so other devices can use the app too.</summary>
    public const string BindAllInterfacesPrefix = "http://*";

    public static WebApplication BuildOrbisWebApplication(
        this WebApplicationBuilder builder,
        OrbisRuntimeOptions options)
    {
        ArgumentNullException.ThrowIfNull(builder);
        ArgumentNullException.ThrowIfNull(options);

        builder.WebHost.UseUrls($"{BindAllInterfacesPrefix}:{options.Port}");
        ConfigureLogging(builder, options);

        builder.Services.AddOrbisStream(options);
        builder.Services.AddSingleton<RequestValidator>();
        builder.Services.AddEndpointsApiExplorer();
        // The Java CorsConfiguration allowed any origin, method and header.
        builder.Services.AddCors();
        builder.Services.AddSwaggerGen(swagger => swagger.SwaggerDoc(OpenApiDocumentName, new OpenApiInfo
        {
            Title = "Stream",
            Version = "OPENAPI_3_0"
        }));

        builder.Services.ConfigureHttpJsonOptions(json =>
        {
            json.SerializerOptions.Converters.Add(new LocalDateTimeConverter());
            json.SerializerOptions.Converters.Add(new NullableLocalDateTimeConverter());
            json.SerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.Never;
        });

        var app = builder.Build();
        ConfigurePipeline(app, options);
        return app;
    }

    /// <summary>
    /// The default host already writes console plus <c>logging:file:path</c>; the value is set
    /// before the host is built so the log file lands in the resolved data directory, like the
    /// <c>logging.file.name</c> property of the Java configuration did.
    /// </summary>
    private static void ConfigureLogging(WebApplicationBuilder builder, OrbisRuntimeOptions options)
    {
        builder.Configuration["logging:file:path"] = options.LogFilePath;
        builder.Logging.SetMinimumLevel(LogLevel.Information);
    }

    private static void ConfigurePipeline(WebApplication app, OrbisRuntimeOptions options)
    {
        app.Environment.WebRootFileProvider = new PhysicalFileProvider(options.WebRootPath);

        app.UseMiddleware<ExceptionHandlingMiddleware>();

        app.UseCors(cors => cors
            .AllowAnyOrigin()
            .AllowAnyMethod()
            .AllowAnyHeader());

        app.UseStaticFiles(new StaticFileOptions
        {
            ServeUnknownFileTypes = false,
            OnPrepareResponse = context =>
            {
                var isIndex = context.File.Name.Equals("index.html", StringComparison.OrdinalIgnoreCase);
                var isAsset = context.Context.Request.Path.StartsWithSegments("/assets", StringComparison.OrdinalIgnoreCase);

                if (isIndex)
                {
                    context.Context.Response.Headers.CacheControl = "no-cache";
                }
                else if (isAsset)
                {
                    context.Context.Response.Headers.CacheControl = "public,max-age=10368000,immutable";
                }
            }
        });

        // springdoc served the document at /documentazione and the UI at /swagger-ui.html. The
        // document is written by an endpoint because the Swashbuckle middleware only answers when
        // the route template contains the {documentName} placeholder, and the SPA fallback would
        // swallow /documentazione with index.html.
        app.MapGet(OpenApiDocumentPath, (ISwaggerProvider provider) => Results.Text(
            provider.GetSwagger(OpenApiDocumentName).SerializeAsJson(OpenApiSpecVersion.OpenApi3_0),
            "application/json"));
        app.UseSwaggerUI(ui =>
        {
            ui.SwaggerEndpoint(OpenApiDocumentPath, "Stream");
            ui.RoutePrefix = "swagger-ui";
        });

        app.MapGet(SwaggerUiPath, () => Results.Redirect("/swagger-ui/index.html"));

        app.MapOrbisEndpoints();

        // ReactController redirected the root to the main menu of the client side router.
        app.MapGet("/", () => Results.Redirect("/orbis/mainMenu"));

        // Same forwarding rules as the Java ReactController: every extensionless path serves the
        // React entry point so the client side router can take over.
        app.MapFallbackToFile("index.html");
    }
}
