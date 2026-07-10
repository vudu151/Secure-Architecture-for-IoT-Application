import React, { useEffect, useState } from 'react';
import {
  Card, Row, Col, Tag, Button, Table, Typography, Space, Empty, Spin,
  Modal, Form, Select, DatePicker, message, Tooltip, Badge,
} from 'antd';
import {
  CarOutlined, PlusOutlined, ReloadOutlined, CheckCircleOutlined,
  CloseCircleOutlined, ClockCircleOutlined,
} from '@ant-design/icons';
import { slotApi } from '../../api/slotApi';
import { bookingApi } from '../../api/bookingApi';
import { vehicleApi } from '../../api/vehicleApi';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const slotStatusConfig = {
  AVAILABLE:   { color: '#52c41a', label: 'Trống',        icon: <CheckCircleOutlined /> },
  OCCUPIED:    { color: '#ff4d4f', label: 'Đang dùng',    icon: <CloseCircleOutlined /> },
  RESERVED:    { color: '#faad14', label: 'Đã đặt',       icon: <ClockCircleOutlined /> },
  MAINTENANCE: { color: '#8c8c8c', label: 'Bảo trì',      icon: null },
};

const DriverParkingPage = () => {
  const [slots, setSlots] = useState([]);
  const [allSlots, setAllSlots] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingModal, setBookingModal] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [estimatedPrice, setEstimatedPrice] = useState(0);
  const [form] = Form.useForm();

  const fetchSlots = async () => {
    setLoading(true);
    try {
      const [allRes, availRes] = await Promise.all([
        slotApi.getAllSlots(),
        slotApi.getAvailableSlots(),
      ]);
      const all = allRes.data?.data ?? allRes.data ?? [];
      const avail = availRes.data?.data ?? availRes.data ?? [];
      setAllSlots(Array.isArray(all) ? all : []);
      setSlots(Array.isArray(avail) ? avail : []);
    } catch (e) {
      message.error('Không thể tải danh sách chỗ đỗ');
    } finally {
      setLoading(false);
    }
  };

  const fetchVehicles = async () => {
    try {
      const res = await vehicleApi.getMyVehicles();
      const data = res.data?.data ?? res.data ?? [];
      setVehicles(Array.isArray(data) ? data : []);
    } catch (e) {
      // ignore
    }
  };

  useEffect(() => {
    fetchSlots();
    fetchVehicles();
  }, []);

  const openBooking = (slot) => {
    setSelectedSlot(slot);
    setEstimatedPrice(0);
    form.resetFields();
    setBookingModal(true);
  };

  const handleTimeChange = (dates) => {
    if (!dates || !dates[0] || !dates[1]) {
      setEstimatedPrice(0);
      return;
    }
    const [from, until] = dates;
    const diffHours = until.diff(from, 'hour', true); // get exact hours as float
    const roundedHours = Math.ceil(diffHours); // round up to nearest hour
    const finalHours = roundedHours > 0 ? roundedHours : 1; // minimum 1 hour
    setEstimatedPrice(finalHours * 50000);
  };

  const handleBook = async (values) => {
    setSubmitting(true);
    try {
      const [from, until] = values.timeRange;
      await bookingApi.createBooking({
        slotId: selectedSlot.id,
        vehicleId: values.vehicleId,
        bookedFrom: from.format('YYYY-MM-DDTHH:mm:ss'),
        bookedUntil: until.format('YYYY-MM-DDTHH:mm:ss'),
      });
      message.success('Đặt chỗ thành công!');
      setBookingModal(false);
      fetchSlots();
    } catch (e) {
      const msg = e.response?.data?.message || 'Đặt chỗ thất bại';
      message.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  // Group by zone
  const zones = [...new Set(allSlots.map((s) => s.zone))].sort();
  const availableSet = new Set(slots.map((s) => s.id));

  const slotsByZone = zones.map((zone) => ({
    zone,
    slots: allSlots.filter((s) => s.zone === zone),
  }));

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <Card style={{ marginBottom: 24, borderRadius: 12 }}>
        <Row align="middle" justify="space-between">
          <Col>
            <Title level={4} style={{ margin: 0 }}>
              <CarOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              Bản đồ bãi đỗ xe
            </Title>
            <Text type="secondary">Đơn giá đỗ xe: <strong style={{ color: '#52c41a' }}>50,000 ₫ / Giờ</strong>. Chọn chỗ trống (màu xanh) để đặt.</Text>
          </Col>
          <Col>
            <Space>
              <Badge color="#52c41a" text="Trống" />
              <Badge color="#ff4d4f" text="Đang dùng" />
              <Badge color="#faad14" text="Đã đặt" />
              <Badge color="#8c8c8c" text="Bảo trì" />
              <Button icon={<ReloadOutlined />} onClick={fetchSlots}>
                Làm mới
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Slot grid by zone */}
      {slotsByZone.map(({ zone, slots: zSlots }) => (
        <Card
          key={zone}
          title={`Khu ${zone}`}
          style={{ marginBottom: 16, borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
        >
          <Row gutter={[12, 12]}>
            {zSlots.map((slot) => {
              const isAvailable = availableSet.has(slot.id);
              const status = isAvailable ? 'AVAILABLE' : slot.status || 'OCCUPIED';
              const cfg = slotStatusConfig[status] || slotStatusConfig.OCCUPIED;

              return (
                <Col key={slot.id} xs={6} sm={4} md={3}>
                  <Tooltip title={`${slot.slotCode} — ${cfg.label}`}>
                    <Button
                      type={isAvailable ? 'primary' : 'default'}
                      disabled={!isAvailable}
                      onClick={() => isAvailable && openBooking(slot)}
                      style={{
                        width: '100%',
                        height: 56,
                        borderRadius: 8,
                        background: isAvailable ? '#52c41a' : cfg.color,
                        borderColor: isAvailable ? '#52c41a' : cfg.color,
                        color: '#fff',
                        fontWeight: 'bold',
                        fontSize: 12,
                        opacity: isAvailable ? 1 : 0.7,
                      }}
                    >
                      {slot.slotCode}
                    </Button>
                  </Tooltip>
                </Col>
              );
            })}
          </Row>
        </Card>
      ))}

      {/* Booking Modal */}
      <Modal
        title={`Đặt chỗ ${selectedSlot?.slotCode}`}
        open={bookingModal}
        onCancel={() => setBookingModal(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleBook}>
          <Form.Item
            name="vehicleId"
            label="Chọn xe"
            rules={[{ required: true, message: 'Vui lòng chọn xe!' }]}
          >
            <Select placeholder="Chọn biển số xe">
              {vehicles.map((v) => (
                <Select.Option key={v.id} value={v.id}>
                  {v.licensePlate} — {v.vehicleType}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="timeRange"
            label="Thời gian đặt"
            rules={[{ required: true, message: 'Vui lòng chọn thời gian!' }]}
          >
            <RangePicker
              showTime
              format="DD/MM/YYYY HH:mm"
              disabledDate={(d) => d && d < dayjs().startOf('day')}
              onChange={handleTimeChange}
              style={{ width: '100%' }}
            />
          </Form.Item>

          {estimatedPrice > 0 && (
            <div style={{ marginBottom: 24, padding: '12px 16px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 8 }}>
              <Text>Tạm tính ({estimatedPrice / 50000} giờ): </Text>
              <Text strong style={{ color: '#52c41a', fontSize: 16 }}>{estimatedPrice.toLocaleString('vi-VN')} ₫</Text>
              <br/>
              <Text type="secondary" style={{ fontSize: 12 }}>* Số tiền này sẽ tự động trừ vào ví khi bạn xác nhận.</Text>
            </div>
          )}

          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => setBookingModal(false)}>Hủy</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={submitting}
                icon={<PlusOutlined />}
                style={{ background: '#52c41a', borderColor: '#52c41a' }}
              >
                Xác nhận đặt chỗ
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DriverParkingPage;
