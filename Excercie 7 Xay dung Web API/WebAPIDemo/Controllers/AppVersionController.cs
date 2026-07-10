using Microsoft.AspNetCore.Mvc;
using WebAPIDemo.Models;
using WebAPIDemo.Services;

namespace WebAPIDemo.Controllers
{
    /// <summary>
    /// Controller API quản lý phiên bản ứng dụng/firmware IoT
    /// </summary>
    [Route("api/[controller]")]
    [ApiController]
    public class AppVersionController : ControllerBase
    {
        private readonly AppVersionService _appVersionService;

        public AppVersionController(AppVersionService appVersionService)
        {
            _appVersionService = appVersionService;
        }

        /// <summary>Lấy tất cả phiên bản</summary>
        [HttpGet]
        public async Task<ActionResult<List<AppVersion>>> Get()
        {
            var versions = await _appVersionService.GetAllAsync();
            return Ok(versions);
        }

        /// <summary>Lấy phiên bản theo Id</summary>
        [HttpGet("{id:int}")]
        public async Task<ActionResult<AppVersion>> Get(int id)
        {
            var version = await _appVersionService.GetByIdAsync(id);
            if (version == null)
                return NotFound(new { message = $"Không tìm thấy AppVersion với Id = {id}" });

            return Ok(version);
        }

        /// <summary>Lấy phiên bản mới nhất của một thiết bị</summary>
        [HttpGet("device/{deviceId}/latest")]
        public async Task<ActionResult<AppVersion>> GetLatest(string deviceId)
        {
            var version = await _appVersionService.GetLatestByDeviceIdAsync(deviceId);
            if (version == null)
                return NotFound(new { message = $"Không tìm thấy phiên bản nào cho device '{deviceId}'" });

            return Ok(version);
        }

        /// <summary>Đăng ký phiên bản mới</summary>
        /// <remarks>
        /// Body mẫu:
        /// {
        ///   "deviceId": "DEVICE_001",
        ///   "version": 1
        /// }
        /// </remarks>
        [HttpPost]
        public async Task<ActionResult<AppVersion>> Post(AppVersion appVersion)
        {
            var created = await _appVersionService.CreateAsync(appVersion);
            return CreatedAtAction(nameof(Get), new { id = created.Id }, created);
        }

        /// <summary>Xóa phiên bản theo Id</summary>
        [HttpDelete("{id:int}")]
        public async Task<IActionResult> Delete(int id)
        {
            var deleted = await _appVersionService.DeleteAsync(id);
            if (!deleted)
                return NotFound(new { message = $"Không tìm thấy AppVersion với Id = {id}" });

            return NoContent();
        }
    }
}
