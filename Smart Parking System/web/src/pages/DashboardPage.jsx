import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Space, Typography, Spin, message } from 'antd';
import {
  CarOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { Pie, Column } from '@ant-design/charts';
import { adminApi } from '../api/adminApi';
import { formatVND, formatDateTime } from '../constants';
import { subscribeToTopic, unsubscribeFromTopic } from '../utils/websocket';

const { Title, Text } = Typography;

const DashboardPage = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalSlots: 0,
    occupiedSlots: 0,
    availableSlots: 0,
    reservedSlots: 0,
    revenueToday: 0,
    totalBookingsToday: 0,
  });
  const [recentBookings, setRecentBookings] = useState([]);

  const loadData = async () => {
    try {
      const { data } = await adminApi.getDashboard();
      const dashboardData = data.data || data;
      setStats(dashboardData);
      
      // Mock recent bookings list based on stats
      // In a real app we could load this from a booking API, 
      // but to align with Java Backend Admin API we combine dashboard data.
      // Let's populate some mock bookings for display
      setRecentBookings([
        { id: 1, bookingCode: 'BK7A8902', slotCode: 'A03', vehiclePlate: '30A-987.65', status: 'CHECKED_IN', bookedFrom: new Date(Date.now() - 3600000).toISOString(), createdAt: new Date(Date.now() - 7200000).toISOString() },
        { id: 2, bookingCode: 'BK9C8741', slotCode: 'A07', vehiclePlate: '29K-123.45', status: 'CONFIRMED', bookedFrom: new Date(Date.now() + 1800000).toISOString(), createdAt: new Date(Date.now() - 1200000).toISOString() },
        { id: 3, bookingCode: 'BK3D5210', slotCode: 'B02', vehiclePlate: '51F-654.32', status: 'COMPLETED', bookedFrom: new Date(Date.now() - 14400000).toISOString(), createdAt: new Date(Date.now() - 20000000).toISOString() },
        { id: 4, bookingCode: 'BK1A0098', slotCode: 'B10', vehiclePlate: '30E-888.88', status: 'CANCELLED', bookedFrom: new Date(Date.now() - 3600000).toISOString(), createdAt: new Date(Date.now() - 8000000).toISOString() },
      ]);
      
      setLoading(false);
    } catch (error) {
      setLoading(false);
      message.error('Không thể tải thông tin thống kê');
    }
  };

  useEffect(() => {
    loadData();

    // Subscribe to WebSocket for real-time dashboard refresh
    subscribeToTopic('/topic/slots', (updatedSlot) => {
      console.log('[Dashboard] Slot updated, reloading stats...', updatedSlot);
      loadData();
    });

    return () => {
      unsubscribeFromTopic('/topic/slots');
    };
  }, []);

  // Pie chart config for slot status distribution
  const pieConfig = {
    appendPadding: 10,
    data: [
      { type: 'Trống (Available)', value: stats.availableSlots },
      { type: 'Đang đỗ (Occupied)', value: stats.occupiedSlots },
      { type: 'Đã đặt (Reserved)', value: stats.reservedSlots },
    ],
    angleField: 'value',
    colorField: 'type',
    radius: 0.8,
    color: ['#52c41a', '#ff4d4f', '#faad14'],
    label: {
      type: 'inner',
      offset: '-30%',
      content: ({ percent }) => `${(percent * 100).toFixed(0)}%`,
      style: {
        fontSize: 14,
        textAlign: 'center',
      },
    },
    interactions: [{ type: 'element-active' }],
  };

  // Hourly traffic column chart (Mock last 8 hours data)
  const columnConfig = {
    data: [
      { hour: '08:00', bookings: 5 },
      { hour: '10:00', bookings: 12 },
      { hour: '12:00', bookings: 8 },
      { hour: '14:00', bookings: 15 },
      { hour: '16:00', bookings: 22 },
      { hour: '18:00', bookings: 18 },
      { hour: '20:00', bookings: 10 },
      { hour: '22:00', bookings: 4 },
    ],
    xField: 'hour',
    yField: 'bookings',
    label: {
      position: 'top',
      style: {
        fill: '#FFFFFF',
        opacity: 0.6,
      },
    },
    meta: {
      hour: { alias: 'Giờ' },
      bookings: { alias: 'Lượt xe vào' },
    },
  };

  const bookingColumns = [
    {
      title: 'Mã đặt chỗ',
      dataIndex: 'bookingCode',
      key: 'bookingCode',
      render: (text) => <Text strong style={{ color: '#1a237e' }}>{text}</Text>,
    },
    {
      title: 'Vị trí',
      dataIndex: 'slotCode',
      key: 'slotCode',
      render: (text) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: 'Biển số xe',
      dataIndex: 'vehiclePlate',
      key: 'vehiclePlate',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        let color = 'default';
        let label = status;
        if (status === 'CONFIRMED') { color = 'warning'; label = 'Đã đặt'; }
        else if (status === 'CHECKED_IN') { color = 'success'; label = 'Đã vào bãi'; }
        else if (status === 'COMPLETED') { color = 'blue'; label = 'Hoàn thành'; }
        else if (status === 'CANCELLED') { color = 'error'; label = 'Đã hủy'; }
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: 'Thời gian đặt',
      dataIndex: 'bookedFrom',
      key: 'bookedFrom',
      render: (text) => formatDateTime(text),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px 0' }}>
        <Spin size="large" tip="Đang tải dữ liệu..." />
      </div>
    );
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {/* Overview Cards */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Tổng số chỗ đỗ"
              value={stats.totalSlots}
              prefix={<CarOutlined style={{ color: '#1a237e', marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Chỗ đỗ trống"
              value={stats.availableSlots}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined style={{ marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Chỗ đỗ đã đặt trước"
              value={stats.reservedSlots}
              valueStyle={{ color: '#faad14' }}
              prefix={<ClockCircleOutlined style={{ marginRight: 8 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card glass-card" bordered={false}>
            <Statistic
              title="Doanh thu hôm nay"
              value={stats.revenueToday}
              formatter={(val) => formatVND(val)}
              valueStyle={{ color: '#008080', fontWeight: 'bold' }}
              prefix={<DollarOutlined style={{ marginRight: 8 }} />}
            />
          </Card>
        </Col>
      </Row>

      {/* Charts Section */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card title="Phân bố chỗ đỗ" className="glass-card" bordered={false}>
            <Pie {...pieConfig} style={{ height: 260 }} />
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="Lưu lượng xe vào trong ngày" className="glass-card" bordered={false}>
            <Column {...columnConfig} style={{ height: 260 }} />
          </Card>
        </Col>
      </Row>

      {/* Recent Bookings Table */}
      <Card title="Giao dịch đặt chỗ gần đây" className="glass-card" bordered={false}>
        <Table
          dataSource={recentBookings}
          columns={bookingColumns}
          rowKey="id"
          pagination={false}
          size="middle"
        />
      </Card>
    </Space>
  );
};

export default DashboardPage;
