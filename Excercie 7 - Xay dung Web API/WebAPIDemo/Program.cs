using Microsoft.EntityFrameworkCore;
using WebAPIDemo.Models;
using WebAPIDemo.Services;

var builder = WebApplication.CreateBuilder(args);

// ====================================================
// 1. Đăng ký Controllers
// ====================================================
builder.Services.AddControllers();

// ====================================================
// 2. Cấu hình SQL Server (thay cho MongoDB)
// Connection string được lấy từ appsettings.json
// ====================================================
builder.Services.AddDbContext<WebAPIDemoContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("SqlServerConnection")));

// ====================================================
// 3. Đăng ký Services (Dependency Injection)
// ====================================================
builder.Services.AddScoped<SensorDataService>();
builder.Services.AddScoped<AppVersionService>();

// ====================================================
// 4. Cấu hình Swagger/OpenAPI
// ====================================================
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new Microsoft.OpenApi.Models.OpenApiInfo
    {
        Title = "WebAPIDemo - IoT Sensor API",
        Version = "v1",
        Description = "API quản lý dữ liệu cảm biến IoT sử dụng SQL Server",
        Contact = new Microsoft.OpenApi.Models.OpenApiContact
        {
            Name = "IoT Team"
        }
    });
    // Bật XML comments cho Swagger (nếu muốn hiện mô tả API)
    var xmlFile = $"{System.Reflection.Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    if (File.Exists(xmlPath))
        c.IncludeXmlComments(xmlPath);
});

// ====================================================
// 5. Cấu hình CORS (cho phép các client khác gọi API)
// ====================================================
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// ====================================================
// 6. Tự động tạo/migrate database khi khởi động
// (Có retry để chờ SQL Server Docker sẵn sàng)
// ====================================================
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<WebAPIDemoContext>();
    var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();

    const int maxRetries = 10;
    for (int attempt = 1; attempt <= maxRetries; attempt++)
    {
        try
        {
            db.Database.EnsureCreated();
            logger.LogInformation("✅ Kết nối SQL Server thành công! Database đã sẵn sàng.");
            break;
        }
        catch (Exception ex) when (attempt < maxRetries)
        {
            logger.LogWarning("⏳ SQL Server chưa sẵn sàng (lần thử {Attempt}/{Max}): {Message}",
                attempt, maxRetries, ex.Message.Split('\n')[0]);
            await Task.Delay(3000); // Chờ 3 giây rồi thử lại
        }
        catch (Exception ex)
        {
            logger.LogError("❌ Không thể kết nối SQL Server sau {Max} lần thử: {Message}",
                maxRetries, ex.Message.Split('\n')[0]);
            logger.LogError("   👉 Hãy chạy: docker compose up -d (trong thư mục cha)");
            // Không crash app - vẫn khởi động để Swagger có thể truy cập được
        }
    }
}

// ====================================================
// 7. Middleware pipeline
// ====================================================
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "WebAPIDemo v1");
    c.RoutePrefix = "swagger"; // Truy cập tại /swagger
});

// Tắt HTTPS redirect để tránh lỗi khi test local
// app.UseHttpsRedirection();

app.UseCors();

app.UseAuthorization();

app.MapControllers();

app.Run();
