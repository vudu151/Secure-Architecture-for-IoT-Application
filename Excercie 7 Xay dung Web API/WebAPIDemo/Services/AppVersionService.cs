using Microsoft.EntityFrameworkCore;
using WebAPIDemo.Models;

namespace WebAPIDemo.Services
{
    /// <summary>
    /// Service xử lý nghiệp vụ cho AppVersion với SQL Server
    /// </summary>
    public class AppVersionService
    {
        private readonly WebAPIDemoContext _context;

        public AppVersionService(WebAPIDemoContext context)
        {
            _context = context;
        }

        /// <summary>Lấy tất cả phiên bản</summary>
        public async Task<List<AppVersion>> GetAllAsync()
        {
            return await _context.AppVersions
                .OrderByDescending(v => v.UpdatedAt)
                .ToListAsync();
        }

        /// <summary>Lấy phiên bản theo Id</summary>
        public async Task<AppVersion?> GetByIdAsync(int id)
        {
            return await _context.AppVersions.FindAsync(id);
        }

        /// <summary>Lấy phiên bản mới nhất của một thiết bị</summary>
        public async Task<AppVersion?> GetLatestByDeviceIdAsync(string deviceId)
        {
            return await _context.AppVersions
                .Where(v => v.DeviceId == deviceId)
                .OrderByDescending(v => v.Version)
                .FirstOrDefaultAsync();
        }

        /// <summary>Thêm phiên bản mới</summary>
        public async Task<AppVersion> CreateAsync(AppVersion appVersion)
        {
            appVersion.UpdatedAt = DateTime.Now;
            _context.AppVersions.Add(appVersion);
            await _context.SaveChangesAsync();
            return appVersion;
        }

        /// <summary>Xóa phiên bản</summary>
        public async Task<bool> DeleteAsync(int id)
        {
            var version = await _context.AppVersions.FindAsync(id);
            if (version == null) return false;

            _context.AppVersions.Remove(version);
            await _context.SaveChangesAsync();
            return true;
        }
    }
}
