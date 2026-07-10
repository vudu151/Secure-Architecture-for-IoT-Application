using Microsoft.EntityFrameworkCore;

namespace WebAPIDemo.Models
{
    /// <summary>
    /// DbContext cho SQL Server - quản lý kết nối và các bảng trong DB
    /// </summary>
    public class WebAPIDemoContext : DbContext
    {
        public WebAPIDemoContext(DbContextOptions<WebAPIDemoContext> options)
            : base(options)
        {
        }

        // Bảng SensorData trong SQL Server
        public DbSet<SensorData> SensorDatas { get; set; } = null!;

        // Bảng AppVersions trong SQL Server
        public DbSet<AppVersion> AppVersions { get; set; } = null!;

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            // Cấu hình bảng SensorData
            modelBuilder.Entity<SensorData>(entity =>
            {
                entity.HasKey(e => e.Id);
                entity.Property(e => e.DeviceId).IsRequired().HasMaxLength(100);
                entity.Property(e => e.Temperature).HasColumnType("float");
                entity.Property(e => e.Humidity).HasColumnType("float");
                entity.Property(e => e.CreatedAt).HasDefaultValueSql("GETDATE()");
            });

            // Cấu hình bảng AppVersions
            modelBuilder.Entity<AppVersion>(entity =>
            {
                entity.HasKey(e => e.Id);
                entity.Property(e => e.DeviceId).IsRequired().HasMaxLength(100);
                entity.Property(e => e.UpdatedAt).HasDefaultValueSql("GETDATE()");
            });
        }
    }
}
