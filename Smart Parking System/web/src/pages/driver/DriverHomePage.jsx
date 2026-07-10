import React, { useEffect, useState } from 'react';
import {
  Card, Row, Col, Statistic, Typography, Tag, Table, Empty, Spin, Avatar, Badge,
} from 'antd';
import {
  CarOutlined,
  WalletOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import useAuthStore from '../../stores/authStore';
import { walletApi } from '../../api/walletApi';
import { bookingApi } from '../../api/bookingApi';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const statusConfig = {
  PENDING:    { color: 'gold',    label: 'Đang chờ',    icon: <ClockCircleOutlined /> },
  CONFIRMED:  { color: 'blue',    label: 'Đã xác nhận', icon: <CheckCircleOutlined /> },
  CHECKED_IN: { color: 'green',   label: 'Đã vào bãi',  icon: <CheckCircleOutlined /> },
  COMPLETED:  { color: 'default', label: 'Hoàn thành',  icon: <CheckCircleOutlined /> },
  CANCELLED:  { color: 'red',     label: 'Đã hủy',      icon: null },
  EXPIRED:    { color: 'default', label: 'Hết hạn',     icon: null },
};

const DriverHomePage = () => {
  const { user } = useAuthStore();
  const [balance, setBalance] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [balRes, bookRes] = await Promise.all([
          walletApi.getBalance(),
          bookingApi.getMyBookings({ page: 0, size: 5 }),
        ]);
        setBalance(balRes.data?.data ?? balRes.data);
        const raw = bookRes.data?.data ?? bookRes.data;
        setBookings(Array.isArray(raw) ? raw.slice(0, 5) : (raw?.content ?? []));
      } catch (e) {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const activeBooking = bookings.find(
    (b) => b.status === 'CONFIRMED' || b.status === 'CHECKED_IN' || b.status === 'PENDING'
  );

  const columns = [
    {
      title: 'Mã đặt chỗ',
      dataIndex: 'bookingCode',
      key: 'bookingCode',
      render: (code) => <Text code>{code}</Text>,
    },
    {
      title: 'Chỗ đỗ',
      dataIndex: 'slotCode',
      key: 'slotCode',
      render: (v) => <Tag color="blue">{v || '-'}</Tag>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const cfg = statusConfig[status] || { color: 'default', label: status };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: 'Thời gian đặt',
      dataIndex: 'bookedFrom',
      key: 'bookedFrom',
      render: (v) => v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-',
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (v) =>
        v != null ? `${Number(v).toLocaleString('vi-VN')} ₫` : '-',
    },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '0 4px' }}>
      {/* Welcome */}
      <div
        style={{
          background: 'linear-gradient(135deg, #1a237e 0%, #0d47a1 100%)',
          borderRadius: 16,
          padding: '32px 40px',
          marginBottom: 24,
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          gap: 24,
        }}
      >
        <Avatar
          size={72}
          style={{ background: 'rgba(255,255,255,0.2)', fontSize: 32 }}
          icon={<CarOutlined />}
        />
        <div>
          <Title level={3} style={{ color: '#fff', margin: 0 }}>
            Xin chào, {user?.fullName || 'Tài xế'}! 👋
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.8)' }}>
            Chào mừng bạn đến với Smart Parking System
          </Text>
        </div>
      </div>

      {/* Stats */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card
            style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
          >
            <Statistic
              title="Số dư ví"
              value={balance != null ? balance : 0}
              suffix="₫"
              formatter={(v) => Number(v).toLocaleString('vi-VN')}
              prefix={<WalletOutlined style={{ color: '#1677ff' }} />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
            <Statistic
              title="Tổng lần đặt chỗ"
              value={bookings.length}
              prefix={<CalendarOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
            <Statistic
              title="Trạng thái hiện tại"
              value={activeBooking ? 'Đang đặt chỗ' : 'Chưa đặt chỗ'}
              prefix={
                activeBooking
                  ? <CheckCircleOutlined style={{ color: '#52c41a' }} />
                  : <ClockCircleOutlined style={{ color: '#faad14' }} />
              }
              valueStyle={{ color: activeBooking ? '#52c41a' : '#faad14', fontSize: 18 }}
            />
          </Card>
        </Col>
      </Row>

      {/* Active booking banner */}
      {activeBooking && (
        <Card
          style={{
            marginBottom: 24,
            borderRadius: 12,
            border: '1px solid #52c41a',
            background: '#f6ffed',
          }}
        >
          <Row align="middle" gutter={16}>
            <Col>
              <Badge status="processing" color="green" />
            </Col>
            <Col flex="auto">
              <Text strong style={{ color: '#52c41a' }}>Đặt chỗ đang hoạt động: </Text>
              <Text code>{activeBooking.bookingCode}</Text>
              {' — '}
              <Tag color={statusConfig[activeBooking.status]?.color}>
                {statusConfig[activeBooking.status]?.label}
              </Tag>
            </Col>
          </Row>
        </Card>
      )}

      {/* Recent bookings */}
      <Card
        title="Đặt chỗ gần đây"
        style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
      >
        {bookings.length === 0 ? (
          <Empty description="Bạn chưa có lần đặt chỗ nào" />
        ) : (
          <Table
            dataSource={bookings}
            columns={columns}
            rowKey="id"
            pagination={false}
            size="small"
          />
        )}
      </Card>
    </div>
  );
};

export default DriverHomePage;
